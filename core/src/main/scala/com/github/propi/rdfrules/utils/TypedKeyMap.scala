package com.github.propi.rdfrules.utils

import com.github.propi.rdfrules.utils.TypedKeyMap.{Key, Value}

/**
  * Created by Vaclav Zeman on 14. 3. 2018.
  */
sealed trait TypedKeyMap[T <: Value] {
  protected def hmap: collection.Map[Key[T], T]

  def apply[A <: T](implicit key: Key[A]): A = hmap(key).asInstanceOf[A]

  def get[A <: T](implicit key: Key[A]): Option[A] = hmap.get(key).map(_.asInstanceOf[A])

  def exists[A <: T](implicit key: Key[A]): Boolean = hmap.contains(key)

  def iterator: Iterator[T] = hmap.valuesIterator
}

object TypedKeyMap {
  class Mutable[T <: Value] private[TypedKeyMap](m: collection.mutable.Map[Key[T], T]) extends TypedKeyMap[T] {
    protected def hmap: collection.Map[Key[T], T] = m

    def +=(value: T): this.type = {
      m.update(value.companion.asInstanceOf[Key[T]], value)
      this
    }

    def toImmutable: Immutable[T] = new WrappedMap(m)
    //def ++=(col: TypedKeyMap.Immutable[T]): TypedKeyMap[T] = this += (col.iterator.map(x => x.companion.asInstanceOf[Key[T]] -> x).toSeq: _*)

    //def ++=(col: IterableOnce[T]): TypedKeyMap[T] = this ++= TypedKeyMap(col)
  }

  object Mutable {
    def apply[T <: Value](values: T*): Mutable[T] = apply(values)

    def apply[T <: Value](col: IterableOnce[T]): Mutable[T] = new Mutable(collection.mutable.HashMap.from(col.iterator.map(x => x.companion.asInstanceOf[Key[T]] -> x)))
  }

  sealed trait Immutable[T <: Value] extends TypedKeyMap[T] {
    def +(value: T): Immutable[T]

    def -(key: Key[T]): Immutable[T]
  }

  class WrappedMap[T <: Value] private[TypedKeyMap](m: collection.Map[Key[T], T]) extends Immutable[T] {
    protected def hmap: collection.Map[Key[T], T] = m

    def +(value: T): Immutable[T] = new ImmutableMap(m.toMap.updated(value.companion.asInstanceOf[Key[T]], value))

    def -(key: Key[T]): Immutable[T] = new ImmutableMap(m.toMap - key)
  }

  class ImmutableMap[T <: Value] private[TypedKeyMap](m: Map[Key[T], T]) extends Immutable[T] {
    protected def hmap: collection.Map[Key[T], T] = m

    def +(value: T): Immutable[T] = new ImmutableMap(m.updated(value.companion.asInstanceOf[Key[T]], value))

    def -(key: Key[T]): Immutable[T] = new ImmutableMap(m - key)
  }

  trait Value {
    def companion: TypedKeyMap.Key[Value]
  }

  trait Key[+T <: Value]

  def apply[T <: Value](values: T*): Immutable[T] = apply(values)

  def apply[T <: Value](col: IterableOnce[T]): Immutable[T] = new ImmutableMap(Map.from(col.iterator.map(x => x.companion.asInstanceOf[Key[T]] -> x)))
}