package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.thoughtworks.binding.Binding.Constants

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
object Workspace {

  sealed trait FileValue

  object FileValue {

    case class File(name: String, path: String) extends FileValue

    case class Directory(name: String, files: Constants[FileValue]) extends FileValue

  }

  private def anyToFileValue(parent: String, x: js.Dynamic): FileValue = {
    val name = x.name.asInstanceOf[String]
    x.selectDynamic("subfiles").asInstanceOf[js.UndefOr[js.Array[js.Dynamic]]].toOption match {
      case Some(files) => FileValue.Directory(name, Constants(files.map(anyToFileValue(parent + "/" + name, _)): _*))
      case None => FileValue.File(name, parent + "/" + name)
    }
  }

  def loadFiles: Future[Constants[FileValue]] = {
    val result = Promise[Constants[FileValue]]()
    Endpoint.get[js.Array[js.Dynamic]]("/workspace") { data =>
      result.success(Constants(data.data.map(anyToFileValue("", _)): _*))
    }
    result.future
  }

}
