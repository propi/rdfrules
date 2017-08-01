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

  def serialize[T](v: T)(implicit serializer: Serializer[T]): Array[Byte] = serializer.serialize(v)

  def mapOutputStream[T](f: OutputStream)(implicit serializer: Serializer[T], serializationSize: SerializationSize[T]): T => Unit = if (serializationSize.size > 0) {
    v => f.write(serializer.serialize(v))
  } else {
    v => {
      val x = serializer.serialize(v)
      f.write(ByteBuffer.allocate(4 + x.length).putInt(x.length).put(x).array())
    }
  }

  implicit def traversableSerializer[T](implicit serializer: Serializer[T], serializationSize: SerializationSize[T]): Serializer[Traversable[T]] = (v: Traversable[T]) => {
    val baos = new ByteArrayOutputStream()
    val write = mapOutputStream(baos)
    v.foreach(write)
    baos.toByteArray
  }

}
