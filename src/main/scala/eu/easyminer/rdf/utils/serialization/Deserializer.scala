package eu.easyminer.rdf.utils.serialization

import java.io.{ByteArrayInputStream, InputStream}

import eu.easyminer.rdf.utils.NumericByteArray._

import scala.math.Numeric.IntIsIntegral

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
trait Deserializer[T] {

  def deserialize(v: Array[Byte]): T

}

object Deserializer {

  trait Reader[T] {
    def read(): Option[T]
  }

  def deserialize[T](is: InputStream)(implicit deserializer: Deserializer[T], serializationSize: SerializationSize[T]): Option[T] = {
    if (serializationSize.size > 0) {
      val readBytes = new Array[Byte](serializationSize.size)
      if (is.read(readBytes) == -1) {
        None
      } else {
        Some(deserializer.deserialize(readBytes))
      }
    } else {
      deserialize[Int](is)flatMap(size => deserialize(is)(deserializer, SerializationSize[T](size)))
    }
  }

  def deserializeFromInputStream[T, R](buildInputStream: => InputStream)
                                      (f: Reader[T] => R)
                                      (implicit deserializer: Deserializer[T], serializationSize: SerializationSize[T]): R = {
    val is = buildInputStream
    try {
      f(() => deserialize[T](is))
    } finally {
      is.close()
    }
  }

  implicit def traversableDeserializer[T](implicit deserializer: Deserializer[T], serializationSize: SerializationSize[T]): Deserializer[Traversable[T]] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    Stream.continually(Deserializer.deserialize[T](bais)).takeWhile(_.nonEmpty).map(_.get).toIndexedSeq
  }

  implicit val stringDeserializer: Deserializer[String] = (v: Array[Byte]) => new String(v, "UTF-8")

  implicit val booleanDeserializer: Deserializer[Boolean] = (v: Array[Byte]) => v.head == 1

  implicit def numberDeserializer[T](implicit n: Numeric[T]): Deserializer[T] = (v: Array[Byte]) => byteArrayToNumber[T](v)

  implicit def tuple2Deserializer[T1, T2](implicit
                                          deserializer1: Deserializer[T1],
                                          serializationSize1: SerializationSize[T1],
                                          deserializer2: Deserializer[T2],
                                          serializationSize2: SerializationSize[T2]): Deserializer[(T1, T2)] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    (Deserializer.deserialize[T1](bais).get, Deserializer.deserialize[T2](bais).get)
  }

}
