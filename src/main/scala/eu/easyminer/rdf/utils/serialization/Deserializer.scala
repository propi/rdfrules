package eu.easyminer.rdf.utils.serialization

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.ByteBuffer

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
trait Deserializer[+T] {

  def deserialize(v: Array[Byte]): T

}

object Deserializer {

  def deserialize[T](v: Array[Byte])(implicit deserializer: Deserializer[T]): T = deserializer.deserialize(v)

  def mapInputStream[T](f: InputStream)(implicit deserializer: Deserializer[T], serializationSize: SerializationSize[T]): () => Option[T] = if (serializationSize.size > 0) {
    () => {
      val readBytes = new Array[Byte](serializationSize.size)
      if (f.read(readBytes) == -1) None else Some(deserializer.deserialize(readBytes))
    }
  } else {
    () => {
      val readHead = new Array[Byte](4)
      if (f.read(readHead) == -1) {
        None
      } else {
        val size = ByteBuffer.wrap(readHead).getInt
        val readBytes = new Array[Byte](size)
        f.read(readBytes)
        Some(deserializer.deserialize(readBytes))
      }
    }
  }

  implicit def traversableDeserializer[T](implicit deserializer: Deserializer[T], serializationSize: SerializationSize[T]): Deserializer[Traversable[T]] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val read = mapInputStream(bais)
    Stream.continually(read()).takeWhile(_.nonEmpty).map(_.get).toIndexedSeq
  }

}
