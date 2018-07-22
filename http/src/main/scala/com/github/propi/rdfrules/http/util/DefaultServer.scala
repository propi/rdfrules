package com.github.propi.rdfrules.http.util

import akka.actor.{ActorSystem, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.Materializer
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.typesafe.scalalogging.Logger
import spray.json.{DeserializationException, JsObject, JsString}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 13. 8. 2017.
  */
trait DefaultServer {

  private val logger = Logger[DefaultServer]

  implicit val actorSystem: ActorSystem
  implicit val materializer: Materializer

  val host: String
  val port: Int
  val rootPath: String
  val stoppingToken: String

  val route: Route

  private val bindingPromise = Promise[Http.ServerBinding]()
  private implicit lazy val ec: ExecutionContext = actorSystem.dispatcher

  final lazy val rootRoute: Route = {
    val rejectionHandler = RejectionHandler.newBuilder().handle {
      case ValidationException(code, msg) => completeErrorMessage(code, msg)
    }.result().withFallback(RejectionHandler.default.mapRejectionResponse {
      case res@HttpResponse(status, _, ent: HttpEntity.Strict, _) =>
        val message = ent.data.utf8String.replaceAll("\"", """\"""")
        res.copy(entity = HttpEntity(ContentTypes.`application/json`, s"""{ "code": "${status.value}", "message": "$message"}"""))
      case x => x
    })
    val exceptionHandler = ExceptionHandler {
      case ValidationException(code, msg) => completeErrorMessage(code, msg)
      case DeserializationException(msg, _, _) => completeErrorMessage(ValidationException.InvalidInputData.code, msg)
      case th =>
        logger.error(th.getMessage, th)
        completeErrorMessage(th.getClass.getSimpleName, th.getMessage, 500)
    }
    val stoppingEndpoint = {
      val st = stoppingToken.trim
      if (st.length > 0) {
        Some(
          path(st) {
            actorSystem.scheduler.scheduleOnce(2 seconds) {
              stop()
            }
            complete("Stopping...")
          }
        )
      } else {
        None
      }
    }
    decodeRequest {
      encodeResponse {
        respondWithHeader(`Access-Control-Allow-Origin`.*) {
          mapResponseEntity(jsonToUtf8JsonEntity) {
            handleRejections(rejectionHandler) {
              handleExceptions(exceptionHandler) {
                pathPrefix(rootPath) {
                  val innerRoute = cancelRejections(classOf[MethodRejection]) {
                    options {
                      extractRequest { request =>
                        respondWithHeaders(
                          List(
                            request.header[`Access-Control-Request-Headers`].map(x => `Access-Control-Allow-Headers`(x.headers)),
                            request.header[`Access-Control-Request-Method`].map(x => `Access-Control-Allow-Methods`(x.method)),
                            Some(`Access-Control-Allow-Credentials`(true))
                          ).flatten
                        ) {
                          complete("")
                        }
                      }
                    }
                  } ~ route
                  stoppingEndpoint.map(innerRoute ~ _).getOrElse(innerRoute)
                }
              }
            }
          }
        }
      }
    }
  }

  private def jsonToUtf8JsonEntity(responseEntity: ResponseEntity): ResponseEntity = if (responseEntity.contentType.mediaType == MediaTypes.`application/json`) {
    responseEntity.withContentType(ContentType.WithCharset(MediaType.applicationWithOpenCharset("json"), HttpCharsets.`UTF-8`))
  } else {
    responseEntity
  }

  private def completeErrorMessage(code: String, msg: String, status: Int = 400) = complete(status, JsObject("code" -> JsString(code), "message" -> JsString(msg)))

  def bind(): Future[Http.ServerBinding] = {
    val bindingFuture = Http().bindAndHandle(RouteResult.route2HandlerFlow(rootRoute), host, port)
    bindingFuture.foreach { serverBinding =>
      println(s"Server online at http://$host:$port/$rootPath")
      bindingPromise.success(serverBinding)
    }
    bindingPromise.future
  }

  def stop(): Future[Terminated] = bindingPromise.future.flatMap { serverBinding =>
    serverBinding.unbind().flatMap(_ => actorSystem.terminate())
  }

}