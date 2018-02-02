package eu.easyminer.rdf.data

import scala.collection.TraversableView

/**
  * Created by Vaclav Zeman on 2. 2. 2018.
  */
trait Transformable[T, Coll] {

  protected def coll: TraversableView[T, Traversable[_]]

  protected def transform(col: TraversableView[T, Traversable[_]]): Coll

  def map(f: T => T): Coll = transform(coll.map(f))

  def filter(f: T => Boolean): Coll = transform(coll.filter(f))

  def slice(from: Int, until: Int): Coll = transform(coll.slice(from, until))

  def take(n: Int): Coll = transform(coll.take(n))

  def drop(n: Int): Coll = transform(coll.drop(n))

}
