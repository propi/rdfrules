package com.github.propi.rdfrules.http.service

import java.io.File

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ContentTypes, Multipart}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MarshallingDirectives.{as, entity}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.github.propi.rdfrules.http
import com.github.propi.rdfrules.http.formats.WorkspaceJsonFormats._
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 22. 7. 2018.
  */
class Workspace {

  val route: Route = pathPrefix("workspace") {
    get {
      pathEnd {
        complete(http.Workspace.getTree)
      } ~ path(Segments) { pathToFile =>
        getFromFile(new File(http.Workspace.path(pathToFile.mkString("/"))), ContentTypes.`application/octet-stream`)
      }
    } ~ post {
      extractExecutionContext { implicit ec =>
        extractMaterializer { implicit mat =>
          withoutSizeLimit {
            entity(as[Multipart.FormData]) { form =>
              val uploading = form.parts.runFoldAsync[(Option[String], Boolean)](None -> false) {
                case ((None, _), part) if part.name == "directory" => Unmarshal(part.entity).to[String].map(x => Some(x) -> false)
                case ((None, _), part) => part.entity.discardBytes().future().map(_ => None -> false)
                case (r@(Some(directory), false), part) =>
                  if (part.name == "file") {
                    http.Workspace.uploadIfWritable(directory, part.filename.getOrElse(""), part.entity.dataBytes).map(_ => r._1 -> true)
                  } else {
                    part.entity.discardBytes().future().map(_ => r)
                  }
                case (r@(Some(_), true), part) => part.entity.discardBytes().future().map(_ => r)
              }.flatMap {
                case (Some(_), true) => Future.successful("uploaded")
                case (Some(_), false) => Future.failed(ValidationException("NoUploadingFile", "No uploading file with name 'file' after the 'directory' field."))
                case (None, _) => Future.failed(ValidationException("NoDirectoryField", "No 'directory' field was specified."))
              }
              complete(uploading)
            }
          }
        }
      }
    } ~ delete {
      path(Segments) { pathToFile =>
        if (http.Workspace.deleteFileIfWritable(pathToFile.mkString("/"))) {
          complete("deleted")
        } else {
          reject
        }
      }
    }
  }


}
