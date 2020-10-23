package com.github.propi.rdfrules.utils

import java.nio.ByteBuffer
import java.util

import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}

sealed trait ByteString {
  def array: Array[Byte]

  def deserialized[T](implicit deserializer: Deserializer[T]): T = deserializer.deserialize(array)
}

object ByteString {

  private object Empty extends ByteString {
    def array: Array[Byte] = Array.empty[Byte]
  }

  private class Simple(val array: Array[Byte]) extends ByteString {
    override def hashCode(): Int = util.Arrays.hashCode(array)

    override def equals(obj: Any): Boolean = obj match {
      case x: Array[Byte] => util.Arrays.equals(array, x)
      case _ => false
    }
  }

  def apply(array: Array[Byte]): ByteString = if (array.isEmpty) Empty else new Simple(array)

  def apply(byteBuffer: ByteBuffer): ByteString = apply(byteBuffer.array())

  def apply[T](x: T)(implicit serializer: Serializer[T]): ByteString = apply(serializer.serialize(x))

}
