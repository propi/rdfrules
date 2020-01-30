package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Endpoint.UploadProgress
import com.github.propi.rdfrules.gui.{Property, Workspace}
import com.github.propi.rdfrules.gui.Workspace.FileValue
import com.github.propi.rdfrules.gui.utils.Validate.{NoValidator, Validator, _}
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div
import org.scalajs.dom.window
import org.scalajs.dom.document
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class ChooseFileFromWorkspace(files: Future[FileValue.Directory],
                              val name: String,
                              val title: String = "Choose a file from the workspace",
                              description: String = "",
                              validator: Validator[String] = NoValidator[String]()) extends Property {

  private implicit val ec: ExecutionContext = ExecutionContext.global

  files.foreach(x => loadedFiles.value = Some(x))

  private val loadedFiles: Var[Option[FileValue.Directory]] = Var(None)
  private val progressBar: Var[UploadProgress] = Var(UploadProgress(0.0, None))
  private val selectedFile: Var[Option[FileValue.File]] = Var(None)

  val descriptionVar: Binding.Var[String] = Var(description)

  def setValue(data: js.Dynamic): Unit = {
    val path = data.asInstanceOf[String]
    selectedFile.value = Some(FileValue.File(path.replaceFirst(".*/", ""), path)())
  }

  def getSelectedFile: Option[FileValue.File] = selectedFile.value

  def validate(): Option[String] = {
    val msg = validator.validate(selectedFile.value.map(_.path).getOrElse("")).errorMsg
    errorMsg.value = msg
    msg
  }

  def toJson: js.Any = selectedFile.value match {
    case Some(file) => file.path
    case None => js.undefined
  }

  private def reload(): Unit = {
    loadedFiles.value = None
    Workspace.loadFiles.foreach(x => loadedFiles.value = Some(x))
  }

  private def uploadToDirectory(directory: FileValue.Directory): Unit = {
    val fileElement = document.createElement("input").asInstanceOf[HTMLInputElement]
    fileElement.`type` = "file"
    fileElement.onchange = _ => {
      loadedFiles.value = None
      progressBar.value = UploadProgress(0.0, None)
      Workspace.uploadFile(fileElement.files(0), directory, up => progressBar.value = up) {
        case Some(th) =>
          errorMsg.value = Some(th.taskError.message)
          reload()
        case None =>
          reload()
      }
    }
    fileElement.click()
  }

  @dom
  private def bindingFileValue(fileValue: FileValue): Binding[Div] = fileValue match {
    case file: FileValue.File =>
      <div class="file">
        <a onclick={_: Event =>
          if (selectedFile.value.exists(_.path == file.path)) {
            selectedFile.value = None
          } else {
            selectedFile.value = Some(file)
          }}>
          {val x = selectedFile.bind
        if (x.contains(file)) {
          <strong>
            {s"${file.name} (${file.prettySize})"}
          </strong>
        } else {
          <span>
            {s"${file.name} (${file.prettySize})"}
          </span>
        }}
        </a>
        <i class="material-icons control" title="download this file" onclick={_: Event => window.location.href = file.link}>save_alt</i>
        <i class={s"material-icons control${if (file.writable) "" else " deleted"}"} onclick={e: Event =>
          if (window.confirm("Do you want to delete this file?")) {
            e.target.asInstanceOf[HTMLElement].classList.add("deleted")
            Workspace.deleteFile(file) {
              case true => reload()
              case false => errorMsg.value = Some("The file can not be deleted.")
            }
          }}>delete</i>
      </div>
    case directory: FileValue.Directory =>
      <div class="directory">
        <div class="name">
          <span>
            {directory.name}
          </span>{if (directory.writable) {
          <i class="material-icons control" title="upload a file" style="cursor: pointer;" onclick={_: Event => uploadToDirectory(directory)}>cloud_upload</i>
        } else {
          <i></i>
        }}
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
      case Some(rootDir) => <div>
        <i class="material-icons refresh" onclick={_: Event => reload()}>refresh</i>{bindingFileValue(rootDir).bind}
      </div>
      case None => progressBar.bind match {
        case up@UploadProgress(loaded, _) if loaded > 0.0 => <div>
          {s"uploading... ${up.loadedInPercent.map("%.2f%%".format(_)).getOrElse(Workspace.humanReadableByteSize(loaded.toLong))}"}
        </div>
        case _ => <div>loading...</div>
      }
    }}
    </div>
  }

}