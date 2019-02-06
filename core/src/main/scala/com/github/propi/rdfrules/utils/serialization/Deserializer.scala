package com.github.propi.rdfrules.utils.serialization

import java.io.{BufferedInputStream, ByteArrayInputStream, EOFException, InputStream}

import scala.math.Numeric.IntIsIntegral
import scala.util.{Failure, Success, Try}
import com.github.propi.rdfrules.utils.NumericByteArray._

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
trait Deserializer[T] {

  def deserialize(v: Array[Byte]): T

}

object Deserializer {

  class DeserializationException(msg: String) extends Exception(msg)

  trait Reader[T] {
    def read(): Option[T]
  }

  def deserialize[T](is: InputStream)(implicit deserializer: Deserializer[T], serializationSize: SerializationSize[T]): T = {
    if (serializationSize.size >= 0) {
      val readBytes = new Array[Byte](serializationSize.size)
      if (serializationSize.size == 0) {
        deserializer.deserialize(readBytes)
      } else if (is.read(readBytes) == -1) {
        throw new EOFException()
      } else {
        deserializer.deserialize(readBytes)
      }
    } else {
      val size = deserialize[Int](is)
      deserialize(is)(deserializer, SerializationSize[T](size))
    }
  }

  def deserializeOpt[T](is: InputStream)(implicit deserializer: Deserializer[T], serializationSize: SerializationSize[T]): Option[T] = Try(deserialize[T](is)) match {
    case Success(x) => Some(x)
    case Failure(_: EOFException) => None
    case Failure(x) => throw x
  }

  def deserializeFromInputStream[T, R](buildInputStream: => InputStream)
                                      (f: Reader[T] => R)
                                      (implicit deserializer: Deserializer[T], serializationSize: SerializationSize[T]): R = {
    val is = new BufferedInputStream(buildInputStream)
    try {
      f(() => deserializeOpt[T](is))
    } finally {
      is.close()
    }
  }

  implicit def traversableDeserializer[T](implicit deserializer: Deserializer[T], serializationSize: SerializationSize[T]): Deserializer[Traversable[T]] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    Stream.continually(deserializeOpt[T](bais)).takeWhile(_.nonEmpty).map(_.get).toIndexedSeq
  }

  implicit val stringDeserializer: Deserializer[String] = (v: Array[Byte]) => new String(v, "UTF-8")

  implicit val booleanDeserializer: Deserializer[Boolean] = (v: Array[Byte]) => v.head == 1

  implicit def numberDeserializer[T](implicit n: Numeric[T]): Deserializer[T] = (v: Array[Byte]) => byteArrayToNumber[T](v)

  implicit def eitherDeserializer[T1, T2](implicit
                                          deserializer1: Deserializer[T1],
                                          serializationSize1: SerializationSize[T1],
                                          deserializer2: Deserializer[T2],
                                          serializationSize2: SerializationSize[T2]): Deserializer[Either[T1, T2]] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    bais.read() match {
      case 1 => Left(Deserializer.deserialize[T1](bais))
      case 2 => Right(Deserializer.deserialize[T2](bais))
      case x => throw new DeserializationException("No deserializer for index: " + x)
    }
  }

  implicit def tuple2Deserializer[T1, T2](implicit
                                          deserializer1: Deserializer[T1],
                                          serializationSize1: SerializationSize[T1],
                                          deserializer2: Deserializer[T2],
                                          serializationSize2: SerializationSize[T2]): Deserializer[(T1, T2)] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    (Deserializer.deserialize[T1](bais), Deserializer.deserialize[T2](bais))
  }

}
