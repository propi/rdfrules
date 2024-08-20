package com.github.propi.rdfrules.data.ops

import com.github.propi.rdfrules.utils.ForEach

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
trait Transformable[T, Coll] {

  protected def coll: ForEach[T]

  protected def transform(col: ForEach[T]): Coll

  def map(f: T => T): Coll = transform(coll.map(f))

  def flatMap(f: T => ForEach[T]): Coll = transform(coll.flatMap(f))

  def filter(f: T => Boolean): Coll = transform(coll.filter(f))

  def filterIndices(indices: Set[Int]): Coll = transform(coll.zipWithIndex.filter(x => indices(x._2)).map(_._1))

  def slice(from: Int, until: Int): Coll = transform(coll.slice(from, until))

  def take(n: Int): Coll = transform(coll.take(n))

  def drop(n: Int): Coll = transform(coll.drop(n))

  def head: T = coll.head

  def headOption: Option[T] = coll.headOption

  def find(f: T => Boolean): Option[T] = coll.find(f)

  lazy val size: Int = coll.size

}