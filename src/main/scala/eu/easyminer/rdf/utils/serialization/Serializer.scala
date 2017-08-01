package eu.easyminer.rdf.utils.serialization

import java.io.{ByteArrayOutputStream, OutputStream}
import java.nio.ByteBuffer

/**
  * Created by Vaclav Zeman on 1. 8. 2017.
  */
trait Serializer[-T] {

  def serialize(v: T): Array[Byte]

}

object Serializer {

  trait Writer[T] {
    def write(v: T)
  }

  def serialize[T](v: T)(implicit serializer: Serializer[T]): Array[Byte] = serializer.serialize(v)

  def mapOutputStream[T](f: OutputStream)(implicit serializer: Serializer[T], serializationSize: SerializationSize[T]): Writer[T] = if (serializationSize.size > 0) {
    (v: T) => f.write(serializer.serialize(v))
  } else {
    (v: T) => {
      val x = serializer.serialize(v)
      f.write(ByteBuffer.allocate(4 + x.length).putInt(x.length).put(x).array())
    }
  }

  implicit def traversableSerializer[T](implicit serializer: Serializer[T], serializationSize: SerializationSize[T]): Serializer[Traversable[T]] = (v: Traversable[T]) => {
    val baos = new ByteArrayOutputStream()
    val writer: Writer[T] = mapOutputStream(baos)
    v.foreach(writer.write)
    baos.toByteArray
  }

}
