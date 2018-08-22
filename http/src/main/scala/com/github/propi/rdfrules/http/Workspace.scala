package com.github.propi.rdfrules.http

import java.io.File

import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.github.propi.rdfrules.http.util.Conf

/**
  * Created by Vaclav Zeman on 22. 7. 2018.
  */
object Workspace {

  private lazy val directory = {
    val strDir = Conf[String](Main.confPrefix + ".workspace").value
    val dir = new File(strDir)
    if (!dir.isDirectory && !dir.mkdirs()) {
      throw ValidationException("InvalidWorkspace", "The workspace directory can not be created.")
    }
    if (!dir.canRead) {
      throw ValidationException("InvalidWorkspace", "The workspace directory is not readable.")
    }
    dir
  }

  def getTree: IndexedSeq[FileTree] = {
    def getTreeInDirectory(directory: File): IndexedSeq[FileTree] = {
      directory.listFiles().iterator.map { file =>
        if (file.isDirectory) {
          FileTree.Directory(file.getName, getTreeInDirectory(file))
        } else {
          FileTree.File(file.getName)
        }
      }.toIndexedSeq.sortBy(x => (if (x.isInstanceOf[FileTree.Directory]) 0 else 1) -> x.name)
    }

    getTreeInDirectory(directory)
  }

  def path(relativePath: String): String = new File(directory, relativePath.trim.replaceAll("(^|/)\\.\\.(/|$)", "")).getAbsolutePath

  sealed trait FileTree {
    val name: String
  }

  object FileTree {

    case class File(name: String) extends FileTree

    case class Directory(name: String, subfiles: IndexedSeq[FileTree]) extends FileTree

  }

}