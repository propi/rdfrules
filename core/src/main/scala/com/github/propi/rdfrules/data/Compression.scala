package com.github.propi.rdfrules.data

import scala.util.Try

/**
  * Created by Vaclav Zeman on 22. 5. 2019.
  */
sealed trait Compression

object Compression {

  case object GZ extends Compression

  case object BZ2 extends Compression

  def apply(extension: String): Compression = extension match {
    case "bz2" => BZ2
    case "gz" => GZ
    case x => throw new IllegalArgumentException(s"Unsupported compression extension: $x")
  }

  def fromPath(path: String): Option[Compression] = Try(apply(path.replaceFirst(".+[.](.+)$", "$1"))).toOption

}