package eu.easyminer.rdf.utils.serialization

import java.io.{ByteArrayOutputStream, OutputStream}

import eu.easyminer.rdf.utils.NumericByteArray._

/**
  * Created by Vaclav Zeman on 1. 8. 2017.
  */
trait Serializer[T] {

  def serialize(v: T): Array[Byte]

}

object Serializer {

  class SerializationException(msg: String) extends Exception(msg)

  trait Writer[T] {
    def write(v: T)
  }

  def serialize[T](v: T)(implicit serializer: Serializer[T], serializationSize: SerializationSize[T]): Array[Byte] = {
    val x = serializer.serialize(v)
    if (serializationSize.size > 0) {
      x
    } else {
      val baos = new ByteArrayOutputStream()
      baos.write(implicitly[Serializer[Int]].serialize(x.length))
      baos.write(x)
      baos.toByteArray
    }
  }

  def serializeToOutputStream[T](buildOutputStream: => OutputStream)
                                (f: Writer[T] => Unit)
                                (implicit serializer: Serializer[T], serializationSize: SerializationSize[T]): Unit = {
    val os = buildOutputStream
    try {
      f((v: T) => os.write(serialize(v)))
    } finally {
      os.close()
    }
  }

  implicit def traversableSerializer[T](implicit serializer: Serializer[T], serializationSize: SerializationSize[T]): Serializer[Traversable[T]] = (v: Traversable[T]) => {
    val baos = new ByteArrayOutputStream()
    v.foreach(x => baos.write(serialize(x)))
    baos.toByteArray
  }

  implicit val stringSerializer: Serializer[String] = (v: String) => v.getBytes("UTF-8")

  implicit val booleanSerializer: Serializer[Boolean] = (v: Boolean) => Array((if (v) 1 else 0): Byte)

  implicit def numberSerializer[T](implicit n: Numeric[T]): Serializer[T] = (v: T) => v

  implicit def tuple2Serializer[T1, T2](implicit
                                        serializer1: Serializer[T1],
                                        serializationSize1: SerializationSize[T1],
                                        serializer2: Serializer[T2],
                                        serializationSize2: SerializationSize[T2]): Serializer[(T1, T2)] = (v: (T1, T2)) => {
    val baos = new ByteArrayOutputStream()
    baos.write(Serializer.serialize(v._1))
    baos.write(Serializer.serialize(v._2))
    baos.toByteArray
  }

  implicit def tuple3Serializer[T1, T2, T3](implicit
                                            serializer1: Serializer[T1],
                                            serializationSize1: SerializationSize[T1],
                                            serializer2: Serializer[T2],
                                            serializationSize2: SerializationSize[T2],
                                            serializer3: Serializer[T3],
                                            serializationSize3: SerializationSize[T3]): Serializer[(T1, T2, T3)] = (v: (T1, T2, T3)) => {
    val baos = new ByteArrayOutputStream()
    baos.write(Serializer.serialize(v._1))
    baos.write(Serializer.serialize(v._2))
    baos.write(Serializer.serialize(v._3))
    baos.toByteArray
  }

}
