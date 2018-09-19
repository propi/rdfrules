package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.Workspace.FileValue
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class ChooseFileFromWorkspace(files: Future[Constants[FileValue]],
                                   val name: String,
                                   val title: String = "Choose a file from the workspace") extends Property {

  private implicit val ec: ExecutionContext = ExecutionContext.global

  files.foreach(x => loadedFiles.value = Some(x))

  private val loadedFiles: Var[Option[Constants[FileValue]]] = Var(None)
  private val selectedFile: Var[Option[FileValue.File]] = Var(None)

  def toJson: js.Any = selectedFile.value match {
    case Some(file) => file.path
    case None => js.undefined
  }

  @dom
  private def bindingFileValue(fileValue: FileValue): Binding[Div] = fileValue match {
    case file: FileValue.File =>
      <div class="file">
        <a onclick={_: Event => selectedFile.value = Some(file)}>
          {val x = selectedFile.bind
        if (x.contains(file)) {
          <strong>
            {file.name}
          </strong>
        } else {
          <span>
            {file.name}
          </span>
        }}
        </a>
      </div>
    case directory: FileValue.Directory =>
      <div class="directory">
        <div class="name">
          {directory.name}
        </div>
        <div class="files">
          {for (file <- directory.files) yield bindingFileValue(file).bind}
        </div>
      </div>
  }

  @dom
  def valueView: Binding[Div] = {
    <div class="choose-file">
      {val x = loadedFiles.bind
    x match {
      case Some(files) => for (file <- files) yield bindingFileValue(file).bind
      case None => Constants(<div>"loading..."</div>)
    }}
    </div>
  }

}