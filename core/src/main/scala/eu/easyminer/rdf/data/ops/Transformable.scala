package eu.easyminer.rdf.data.ops

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

  lazy val size: Int = coll.size

}