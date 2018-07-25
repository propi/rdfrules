package com.github.propi.rdfrules.data.ops

import java.io._

import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

/**
  * Created by Vaclav Zeman on 27. 2. 2018.
  */
trait Cacheable[T, Coll] extends Transformable[T, Coll] {

  self: Coll =>

  protected implicit val serializer: Serializer[T]
  protected implicit val deserializer: Deserializer[T]
  protected implicit val serializationSize: SerializationSize[T]

  /**
    * Cache the entity into the memory and return cached entity (IndexedSeq abstraction is used)
    *
    * @return in memory cached entity
    */
  def cache: Coll = transform(coll.toVector)

  /**
    * Cache the entity through an output stream and return cached entity through an input stream.
    *
    * @param os output stream builder
    * @param is input stream builder
    * @return the cached entity
    */
  def cache(os: => OutputStream, is: => InputStream): Coll = {
    Serializer.serializeToOutputStream[T](os) { writer =>
      coll.foreach(writer.write)
    }
    transform(new Traversable[T] {
      def foreach[U](f: T => U): Unit = {
        Deserializer.deserializeFromInputStream[T, Unit](is) { reader =>
          Stream.continually(reader.read()).takeWhile(_.isDefined).foreach(x => f(x.get))
        }
      }
    })
  }

  def cache(file: File): Coll = cache(new FileOutputStream(file), new FileInputStream(file))

  /**
    * Cache the entity through an output stream and return the original (not cached) entity.
    *  - if you want to return cached entity, use cache(os, is) or in memory cache()
    *
    * @param os output stream builder
    * @return the same entity
    */
  def cache(os: => OutputStream): Unit = {
    Serializer.serializeToOutputStream[T](os) { writer =>
      coll.foreach(writer.write)
    }
  }

}
