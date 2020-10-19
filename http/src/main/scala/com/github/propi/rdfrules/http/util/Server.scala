package com.github.propi.rdfrules.http.util

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.github.propi.rdfrules.http.util.Server.MainMessage
import com.typesafe.scalalogging.Logger
import spray.json.{DeserializationException, JsObject, JsString}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 13. 8. 2017.
  */
class Server(context: ActorContext[MainMessage], route: Route, serverConf: ServerConf) {

  import serverConf._

  private val logger = Logger[Server]

  implicit private val untypedSystem: akka.actor.ActorSystem = context.system.toClassic
  implicit private val ec: ExecutionContext = context.executionContext

  private val bindingPromise = Promise[Http.ServerBinding]()

  final lazy val rootRoute: Route = {
    val rejectionHandler = RejectionHandler.newBuilder().handle {
      case ValidationException(code, msg) => completeErrorMessage(code, msg)
    }.result().withFallback(RejectionHandler.default.mapRejectionResponse {
      case res@HttpResponse(status, _, ent: HttpEntity.Strict, _) =>
        val message = ent.data.utf8String.replaceAll("\"", """\"""")
        res.withEntity(HttpEntity(ContentTypes.`application/json`, s"""{ "code": "${status.value}", "message": "${message.replaceAll("\n", " ")}"}"""))
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
            context.scheduleOnce(2 seconds, context.self, MainMessage.Stop)
            complete("Stopping...")
          }
        )
      } else {
        None
      }
    }
    decodeRequest {
      encodeResponse {
        extractRequest { request =>
          respondWithHeaders(
            List(
              request.header[`Access-Control-Request-Headers`].map(x => `Access-Control-Allow-Headers`(x.headers)),
              request.header[`Access-Control-Request-Method`].map(x => `Access-Control-Allow-Methods`(x.method)),
              request.header[`Access-Control-Expose-Headers`].map(x => `Access-Control-Expose-Headers`(x.headers)),
              Some(`Access-Control-Allow-Credentials`(true)),
              Some(`Access-Control-Allow-Origin`.*)
            ).flatten
          ) {
            mapResponseEntity(jsonToUtf8JsonEntity) {
              handleRejections(rejectionHandler) {
                handleExceptions(exceptionHandler) {
                  pathPrefix(rootPath) {
                    val innerRoute = cancelRejections(classOf[MethodRejection]) {
                      options {
                        complete("")
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
  }

  private def jsonToUtf8JsonEntity(responseEntity: ResponseEntity): ResponseEntity = if (responseEntity.contentType.mediaType == MediaTypes.`application/json`) {
    responseEntity.withContentType(ContentType.WithCharset(MediaType.applicationWithOpenCharset("json"), HttpCharsets.`UTF-8`))
  } else {
    responseEntity
  }

  private def completeErrorMessage(code: String, msg: String, status: Int = 400) = complete(status, JsObject("code" -> JsString(code), "message" -> JsString(msg)))

  def bind(): Future[Http.ServerBinding] = {
    val bindingFuture = Http().newServerAt(host, port).bind(rootRoute)
    bindingFuture.foreach { serverBinding =>
      println(s"Server online at http://$host:$port/$rootPath")
      bindingPromise.success(serverBinding)
    }
    bindingPromise.future
  }

  def stop(): Unit = bindingPromise.future.foreach { serverBinding =>
    serverBinding.unbind().foreach(_ => context.system.terminate())
  }

}

object Server {

  sealed trait MainMessage

  object MainMessage {

    case object Stop extends MainMessage

  }

  def apply(context: ActorContext[MainMessage], route: Route, serverConf: ServerConf): Server = new Server(context, route, serverConf)

}