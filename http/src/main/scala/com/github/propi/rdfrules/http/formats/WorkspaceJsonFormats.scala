package com.github.propi.rdfrules.http.formats

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.util.BasicExceptions.DeserializationIsNotSupported
import spray.json.{RootJsonFormat, _}

/**
  * Created by Vaclav Zeman on 22. 7. 2018.
  */
object WorkspaceJsonFormats extends DefaultJsonProtocol {

  implicit val fileTreeFormat: RootJsonFormat[Workspace.FileTree] = new RootJsonFormat[Workspace.FileTree] {
    def write(obj: Workspace.FileTree): JsValue = obj match {
      case x: Workspace.FileTree.File => x.toJson
      case x: Workspace.FileTree.Directory => x.toJson
    }

    def read(json: JsValue): Workspace.FileTree = throw DeserializationIsNotSupported
  }

  implicit val fileFormat: RootJsonFormat[Workspace.FileTree.File] = jsonFormat1(Workspace.FileTree.File)

  implicit val directoryFormat: RootJsonFormat[Workspace.FileTree.Directory] = jsonFormat2(Workspace.FileTree.Directory)

}
