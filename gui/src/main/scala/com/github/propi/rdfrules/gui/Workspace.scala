package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.Endpoint.UploadProgress
import com.github.propi.rdfrules.gui.Task.TaskException
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.thoughtworks.binding.Binding.Vars
import org.scalajs.dom.File
import org.scalajs.dom.raw.FormData

import scala.annotation.tailrec
import scala.concurrent.{Future, Promise}
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
object Workspace {

  def humanReadableByteSize(fileSize: Long): String = {
    if (fileSize <= 0) return "0 B"
    val units: Array[String] = Array("B", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
    val digitGroup: Int = (Math.log10(fileSize.toDouble) / Math.log10(1024)).toInt
    f"${fileSize / Math.pow(1024, digitGroup)}%3.2f ${units(digitGroup)}"
  }

  sealed trait FileValue

  object FileValue {

    case class File(name: String, path: String)(val size: Long = 0L, val writable: Boolean = false, val downloadable: Boolean = false) extends FileValue {
      def link: String = Globals.endpoint + "/workspace" + path

      def prettySize: String = humanReadableByteSize(size)
    }

    case class Directory(name: String, path: String, writable: Boolean, files: Vars[FileValue]) extends FileValue {
      def find(path: String): Option[File] = files.value.view.flatMap {
        case f: File if path == f.path => Some(f)
        case f: Directory if path.startsWith(f.path) => f.find(path)
        case _ => None
      }.headOption

      @tailrec
      final def prependPath(path: String): File = {
        val (prefix, suffix) = path.trim.stripPrefix("/").stripSuffix("/").span(_ != '/')
        val trimmedPrefix = prefix.trim
        val trimmedSuffix = suffix.trim
        require(trimmedPrefix.nonEmpty)
        if (trimmedSuffix.isEmpty) {
          prependFile(trimmedPrefix)
        } else {
          files.value.collectFirst {
            case dir: Directory if dir.name == trimmedPrefix => dir
          } match {
            case Some(dir) => dir.prependPath(trimmedSuffix)
            case None =>
              val newDir = Directory(trimmedPrefix, s"$path/$trimmedPrefix", false, Vars.empty)
              files.value.prepend(newDir)
              newDir.prependPath(trimmedSuffix)
          }
        }
      }

      def prependFile(name: String): File = {
        files.value.collectFirst {
          case file: File if file.name == name => file
        }.getOrElse {
          val newFile = File(name, s"$path/$name")()
          files.value.prepend(newFile)
          newFile
        }
      }
    }

  }

  private def anyToFileValue(x: js.Dynamic, path: String, writable: Boolean): FileValue = {
    val name = x.name.asInstanceOf[String]
    x.selectDynamic("subfiles").asInstanceOf[js.UndefOr[js.Array[js.Dynamic]]].toOption match {
      case Some(files) =>
        val writable = x.selectDynamic("writable").asInstanceOf[Boolean]
        FileValue.Directory(if (name.isEmpty) "workspace" else name, path + name, writable, Vars(files.map(anyToFileValue(_, path + name + "/", writable)).toList: _*))
      case None => FileValue.File(name, path + name)(x.selectDynamic("size").toString, writable, true)
    }
  }

  def loadFiles: Future[FileValue.Directory] = {
    val result = Promise[FileValue.Directory]()
    Endpoint.get[js.Dynamic]("/workspace") { data =>
      result.success(anyToFileValue(data.data, "", false).asInstanceOf[FileValue.Directory])
    }(_.toJson)
    result.future
  }

  def uploadFile(file: File, directory: FileValue.Directory, uploadProgress: UploadProgress => Unit)(callback: Option[TaskException] => Unit): Unit = {
    val formData = new FormData()
    formData.append("directory", directory.path)
    formData.append("file", file)
    Endpoint.postWithAutoContentType[String]("/workspace", formData, uploadProgress) { response =>
      if (response.status == 200) {
        callback(None)
      } else {
        callback(Some(TaskException(response.data)))
      }
    }
  }

  def deleteFile(file: FileValue.File)(callback: Boolean => Unit): Unit = {
    Endpoint.delete[String]("/workspace" + file.path) { response =>
      if (response.status == 200) {
        callback(true)
      } else {
        callback(false)
      }
    }
  }

}
