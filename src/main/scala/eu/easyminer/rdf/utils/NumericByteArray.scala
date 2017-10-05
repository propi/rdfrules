package eu.easyminer.rdf.utils

import java.nio.ByteBuffer

import scala.language.implicitConversions

/**
  * Created by propan on 17. 3. 2017.
  */
trait NumericByteArray {

  implicit def numberToByteArray[T](number: T)(implicit n: Numeric[T]): Array[Byte] = number match {
    case x: Short => ByteBuffer.allocate(2).putShort(x).array()
    case x: Int => ByteBuffer.allocate(4).putInt(x).array()
    case x: Long => ByteBuffer.allocate(8).putLong(x).array()
    case x: Float => ByteBuffer.allocate(4).putFloat(x).array()
    case x: Double => ByteBuffer.allocate(8).putDouble(x).array()
    case _ => throw new IllegalArgumentException
  }

  implicit def byteArrayToNumber[T](byteArray: Array[Byte])(implicit n: Numeric[T]): T = n.one match {
    case _: Short => ByteBuffer.wrap(byteArray).getShort.asInstanceOf[T]
    case _: Int => ByteBuffer.wrap(byteArray).getInt.asInstanceOf[T]
    case _: Long => ByteBuffer.wrap(byteArray).getLong.asInstanceOf[T]
    case _: Float => ByteBuffer.wrap(byteArray).getFloat.asInstanceOf[T]
    case _: Double => ByteBuffer.wrap(byteArray).getDouble.asInstanceOf[T]
    case _ => throw new IllegalArgumentException
  }

}

object NumericByteArray extends NumericByteArray