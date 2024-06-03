package com.github.propi.rdfrules.http

import java.io.File
import java.time.Instant
import java.util.Date
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{FileIO, Source}
import akka.stream.{IOResult, Materializer}
import akka.util.ByteString
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.github.propi.rdfrules.http.util.Conf
import com.typesafe.config.ConfigMemorySize
import com.typesafe.scalalogging.Logger

import scala.annotation.tailrec
import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 22. 7. 2018.
  */
object Workspace {

  private val logger = Logger[Workspace.type]

  private val maxUploadedFileSize = Conf[ConfigMemorySize](Main.confPrefix + ".workspace.max-uploaded-file-size").toOption.map(_.toBytes).getOrElse(0L)
  private val maxFilesInDirectory = Conf[Int](Main.confPrefix + ".workspace.max-files-in-directory").toOption.getOrElse(100)

  private val directory = {
    val strDir = Conf[String](Main.confPrefix + ".workspace.path").value
    val dir = new File(strDir)
    if (!dir.isDirectory && !dir.mkdirs()) {
      throw ValidationException("InvalidWorkspace", "The workspace directory can not be created.")
    }
    if (!dir.canRead) {
      throw ValidationException("InvalidWorkspace", "The workspace directory is not readable.")
    }
    dir
  }

  private val writableDirs = {
    val paths = Conf[Seq[String]](Main.confPrefix + ".workspace.writable.path").value.iterator
    val lifetimes = Conf[Seq[Duration]](Main.confPrefix + ".workspace.writable.lifetime").value.iterator
    paths.zipAll(lifetimes, "", Duration.Inf).map { case (path, lifetime) =>
      val (normPath, recursive) = if (path.endsWith("/*")) {
        path.stripSuffix("/*") -> true
      } else {
        path -> false
      }
      val subDir = new File(directory, normPath)
      if (!subDir.isDirectory && !subDir.mkdirs()) {
        throw ValidationException("InvalidWorkspace", "The workspace sub directory can not be created.")
      }
      WritableDirectory(subDir, lifetime, recursive)
    }.toList
  }

  private def deleteExpired(duration: Duration, tree: IndexedSeq[FileTree]): Unit = {
    tree.foreach {
      case x: FileTree.File =>
        if (new Date(x.file.lastModified()).toInstant.plusSeconds(duration.toSeconds).isBefore(Instant.now())) {
        logger.info(s"The file '${x.file.getCanonicalPath}' was expired and therefore was deleted.")
        x.file.delete()
      }
      case x: FileTree.Directory => deleteExpired(duration, x.subfiles)
    }
  }

  def lifetimeActor: Behavior[Boolean] = Behaviors.setup[Boolean] { context =>
    if (writableDirs.exists(_.lifetime.isFinite)) {
      context.self ! true
      Behaviors.receiveMessage { _ =>
        for (dir <- writableDirs if dir.lifetime.isFinite) {
          deleteExpired(dir.lifetime, getTreeInDirectory(dir.path).asInstanceOf[FileTree.Directory].subfiles)
        }
        context.scheduleOnce(1 minute, context.self, true)
        Behaviors.same
      }
    } else {
      Behaviors.stopped
    }
  }

  private def getTreeInDirectory(directory: File): FileTree = {
    if (directory.isDirectory) {
      val subfiles = directory.listFiles().map(getTreeInDirectory).sortBy(x => (if (x.isInstanceOf[FileTree.Directory]) 0 else 1) -> x.name)
      FileTree.Directory(directory.getName, filePathIsWritable(directory, true), ArraySeq.unsafeWrapArray(subfiles))(directory)
    } else {
      FileTree.File(directory.getName)(directory)
    }
  }

  def getTree: FileTree.Directory = getTreeInDirectory(directory).asInstanceOf[FileTree.Directory].copy(name = "")(directory)

  def path(relativePath: String): String = {
    val file = new File(directory, relativePath.trim.replaceAll("(^|/)\\.\\.(/|$)", ""))
    //val parent = file.getParentFile
    //if (!parent.isDirectory) parent.mkdirs()
    file.getAbsolutePath
  }

  def writablePath(relativePath: String): String = {
    val x = path(relativePath)
    writableFile(new File(x))
    x
  }

  private def writableFile(file: File): File = {
    val parent = file.getParentFile
    if (!parent.isDirectory) parent.mkdirs()
    file
  }

  def uploadIfWritable(directory: String, filename: String, source: Source[ByteString, Any])(implicit ec: ExecutionContext, mat: Materializer): Future[IOResult] = Future {
    val dir = new File(path(directory))
    val normFilename = filename.trim.replaceAll("[^\\p{Alpha}\\p{Digit}.]", "_")
    if (filePathIsWritable(dir, true) && dir.listFiles().count(_.isFile) < maxFilesInDirectory) {
      if (normFilename.nonEmpty && normFilename.length <= 150) {
        val file = writableFile(new File(dir, normFilename))
        val result = source.limitWeighted(maxUploadedFileSize)(_.length).runWith(FileIO.toPath(file.toPath))
        result.failed.foreach(_ => file.delete())
        result
      } else {
        Future.failed(ValidationException("InvalidFileName", "Empty or too long filename."))
      }
    } else {
      Future.failed(ValidationException("DirectoryIsNotWritable", "The workspace directory is not writable."))
    }
  }.flatten

  @tailrec
  private def deleteEmptyFoldersRecursively(file: File): Unit = {
    if (file.isDirectory) {
      if (file.getCanonicalPath != this.directory.getCanonicalPath && !this.writableDirs.exists(_.path.getCanonicalPath == file.getCanonicalPath) && file.delete()) {
        deleteEmptyFoldersRecursively(file.getParentFile)
      }
    } else {
      deleteEmptyFoldersRecursively(file.getParentFile)
    }
  }

  def deleteFileIfWritable(filePath: String): Boolean = {
    val file = new File(path(filePath))
    if (file.isFile && filePathIsWritable(file, false) && file.delete()) {
      deleteEmptyFoldersRecursively(file)
      true
    } else {
      false
    }
  }

  def filePathIsWritable(path: String, isDir: Boolean): Boolean = {
    filePathIsWritable(new File(Workspace.path(path)), isDir)
  }

  def filePathIsWritable(path: File, isDir: Boolean): Boolean = {
    writableDirs.exists(x => (x.recursive && path.getCanonicalPath.startsWith(s"${x.path.getCanonicalPath}${File.separator}")) || (!isDir && x.path.getCanonicalPath == path.getParentFile.getCanonicalPath) || x.path.getCanonicalPath == path.getCanonicalPath)
  }

  sealed trait FileTree {
    val name: String
  }

  object FileTree {

    case class File(name: String)(val file: java.io.File) extends FileTree

    case class Directory(name: String, writable: Boolean, subfiles: IndexedSeq[FileTree])(val file: java.io.File) extends FileTree

  }

  case class WritableDirectory(path: File, lifetime: Duration, recursive: Boolean)

}