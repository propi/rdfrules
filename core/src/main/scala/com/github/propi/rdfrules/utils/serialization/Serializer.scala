package com.github.propi.rdfrules.utils.serialization

import java.io.{BufferedOutputStream, ByteArrayOutputStream, OutputStream}
import com.github.propi.rdfrules.utils.NumericByteArray._

/**
  * Created by Vaclav Zeman on 1. 8. 2017.
  */
trait Serializer[T] {

  def serialize(v: T): Array[Byte]

}

object Serializer {

  class SerializationException(msg: String) extends Exception(msg)

  trait Writer[T] {
    def write(v: T): Unit
  }

  def serialize[T](v: T)(implicit serializer: Serializer[T], serializationSize: SerializationSize[T]): Array[Byte] = {
    val x = serializer.serialize(v)
    if (serializationSize.size >= 0) {
      x
    } else {
      val baos = new ByteArrayOutputStream()
      baos.write(implicitly[Serializer[Int]].serialize(x.length))
      baos.write(x)
      baos.toByteArray
    }
  }

  def directSerialize[T](v: T)(implicit serializer: Serializer[T]): Array[Byte] = {
    serializer.serialize(v)
  }

  def by[A, B](f: A => B)(implicit serializer: Serializer[B]): Serializer[A] = (v: A) => serializer.serialize(f(v))

  def serializeToOutputStream[T](buildOutputStream: => OutputStream)
                                (f: Writer[T] => Unit)
                                (implicit serializer: Serializer[T], serializationSize: SerializationSize[T]): Unit = {
    val os = new BufferedOutputStream(buildOutputStream)
    try {
      f((v: T) => os.write(serialize(v)))
    } finally {
      os.close()
    }
  }

  implicit def iteratorSerializer[T](implicit serializer: Serializer[T], serializationSize: SerializationSize[T]): Serializer[Iterator[T]] = (v: Iterator[T]) => {
    val baos = new ByteArrayOutputStream()
    v.foreach(x => baos.write(serialize(x)))
    baos.toByteArray
  }

  implicit val stringSerializer: Serializer[String] = (v: String) => v.getBytes("UTF-8")

  implicit val booleanSerializer: Serializer[Boolean] = (v: Boolean) => Array((if (v) 1 else 0): Byte)

  implicit def numberSerializer[T](implicit n: Numeric[T]): Serializer[T] = (v: T) => v

  implicit def eitherSerializer[T1, T2](implicit
                                        serializer1: Serializer[T1],
                                        serializationSize1: SerializationSize[T1],
                                        serializer2: Serializer[T2],
                                        serializationSize2: SerializationSize[T2]): Serializer[Either[T1, T2]] = (v: Either[T1, T2]) => {
    val baos = new ByteArrayOutputStream()
    v match {
      case Left(x) =>
        baos.write(1)
        baos.write(Serializer.serialize(x))
      case Right(x) =>
        baos.write(2)
        baos.write(Serializer.serialize(x))
    }
    baos.toByteArray
  }

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

  implicit def tuple4Serializer[T1, T2, T3, T4](implicit
                                                serializer1: Serializer[T1],
                                                serializationSize1: SerializationSize[T1],
                                                serializer2: Serializer[T2],
                                                serializationSize2: SerializationSize[T2],
                                                serializer3: Serializer[T3],
                                                serializationSize3: SerializationSize[T3],
                                                serializer4: Serializer[T4],
                                                serializationSize4: SerializationSize[T4]): Serializer[(T1, T2, T3, T4)] = (v: (T1, T2, T3, T4)) => {
    val baos = new ByteArrayOutputStream()
    baos.write(Serializer.serialize(v._1))
    baos.write(Serializer.serialize(v._2))
    baos.write(Serializer.serialize(v._3))
    baos.write(Serializer.serialize(v._4))
    baos.toByteArray
  }

}
