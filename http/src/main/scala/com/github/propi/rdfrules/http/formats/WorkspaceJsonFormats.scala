package com.github.propi.rdfrules.http.formats

import com.github.propi.rdfrules.http.Workspace.FileTree
import com.github.propi.rdfrules.http.util.BasicExceptions.DeserializationIsNotSupported
import spray.json.{RootJsonFormat, _}

/**
  * Created by Vaclav Zeman on 22. 7. 2018.
  */
object WorkspaceJsonFormats extends DefaultJsonProtocol {

  implicit val fileTreeFormat: RootJsonFormat[FileTree] = new RootJsonFormat[FileTree] {
    def write(obj: FileTree): JsValue = obj match {
      case x: FileTree.File => x.toJson
      case x: FileTree.Directory => x.toJson
    }

    def read(json: JsValue): FileTree = throw DeserializationIsNotSupported
  }

  implicit val fileFormat: RootJsonFormat[FileTree.File] = new RootJsonFormat[FileTree.File] {
    def read(json: JsValue): FileTree.File = throw DeserializationIsNotSupported

    def write(obj: FileTree.File): JsValue = JsObject("name" -> obj.name.toJson, "size" -> obj.file.length().toJson)
  }


  implicit val directoryFormat: RootJsonFormat[FileTree.Directory] = new RootJsonFormat[FileTree.Directory] {
    def read(json: JsValue): FileTree.Directory = throw DeserializationIsNotSupported

    def write(obj: FileTree.Directory): JsValue = JsObject(
      "name" -> obj.name.toJson,
      "writable" -> obj.writable.toJson,
      "subfiles" -> obj.subfiles.toJson
    )
  }

}
