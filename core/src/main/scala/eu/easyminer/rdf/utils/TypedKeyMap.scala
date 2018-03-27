package eu.easyminer.rdf.utils

import eu.easyminer.rdf.utils.TypedKeyMap.{Key, Value}

/**
  * Created by Vaclav Zeman on 14. 3. 2018.
  */
class TypedKeyMap[T <: Value] private(m: collection.mutable.Map[Key[T], T]) extends TypedKeyMap.Immutable[T] {
  def apply[A <: T](implicit key: Key[A]): A = m(key).asInstanceOf[A]

  def get[A <: T](implicit key: Key[A]): Option[A] = m.get(key).map(_.asInstanceOf[A])

  def exists[A <: T](implicit key: Key[A]): Boolean = m.contains(key)

  def +=(keyValues: (Key[T], T)*): TypedKeyMap[T] = {
    m ++= keyValues
    this
  }

  def ++=(col: TypedKeyMap.Immutable[T]): TypedKeyMap[T] = this += (col.iterator.map(x => x.companion.asInstanceOf[Key[T]] -> x).toSeq: _*)

  def iterator: Iterator[T] = m.valuesIterator
}

object TypedKeyMap {

  trait Immutable[T <: Value] {
    def apply[A <: T](implicit key: Key[A]): A

    def get[A <: T](implicit key: Key[A]): Option[A]

    def exists[A <: T](implicit key: Key[A]): Boolean

    def iterator: Iterator[T]
  }

  trait Value {
    def companion: TypedKeyMap.Key[Value]
  }

  trait Key[+T <: Value]

  def apply[T <: Value](keyValues: (Key[T], T)*): TypedKeyMap[T] = new TypedKeyMap(collection.mutable.HashMap(keyValues: _*))

}