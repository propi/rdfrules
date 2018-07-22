package com.github.propi.rdfrules.data.ops

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
trait Transformable[T, Coll] {

  protected def coll: Traversable[T]

  protected def transform(col: Traversable[T]): Coll

  def map(f: T => T): Coll = transform(coll.view.map(f))

  def filter(f: T => Boolean): Coll = transform(coll.view.filter(f))

  def slice(from: Int, until: Int): Coll = transform(coll.view.slice(from, until))

  def take(n: Int): Coll = transform(coll.view.take(n))

  def drop(n: Int): Coll = transform(coll.view.drop(n))

  def head: T = coll.head

  def headOption: Option[T] = coll.headOption

  def find(f: T => Boolean): Option[T] = coll.find(f)

  lazy val size: Int = if (coll.isInstanceOf[IndexedSeq[_]]) {
    coll.size
  } else {
    var i = 0
    for (_ <- coll) i += 1
    i
  }

}