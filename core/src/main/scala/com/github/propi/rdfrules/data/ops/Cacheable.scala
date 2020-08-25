package com.github.propi.rdfrules.data.ops

import java.io._
import java.util
import scala.collection.JavaConverters._

import com.github.propi.rdfrules.utils.Debugger
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

/**
  * Created by Vaclav Zeman on 27. 2. 2018.
  */
trait Cacheable[T, Coll] extends Transformable[T, Coll] {

  self: Coll =>

  protected implicit val serializer: Serializer[T]
  protected implicit val deserializer: Deserializer[T]
  protected implicit val serializationSize: SerializationSize[T]

  protected def cachedTransform(col: Traversable[T]): Coll

  /**
    * Cache the entity into the memory and return cached entity (IndexedSeq abstraction is used)
    * Strict transformation
    *
    * @return in memory cached entity
    */
  def cache(implicit debugger: Debugger): Coll = {
    debugger.debug("Dataset loading") { ad =>
      val buffer = new util.LinkedList[T]()
      for (x <- coll.view.takeWhile(_ => !debugger.isInterrupted)) {
        buffer.add(x)
        ad.done()
      }
      if (debugger.isInterrupted) {
        debugger.logger.warn(s"The loading task has been interrupted. The loaded data may not be complete.")
      }
      cachedTransform(new Traversable[T] {
        def foreach[U](f: T => U): Unit = buffer.iterator().asScala.foreach(f)
      })
    }
  }

  /**
    * Cache the entity through an output stream and return cached entity through an input stream.
    * Streaming transformation
    *
    * @param os output stream builder
    * @param is input stream builder
    * @return the cached entity
    */
  def cache(os: => OutputStream, is: => InputStream)(implicit debugger: Debugger): Coll = {
    cache(os)
    transform(new Traversable[T] {
      def foreach[U](f: T => U): Unit = {
        Deserializer.deserializeFromInputStream[T, Unit](is) { reader =>
          Stream.continually(reader.read()).takeWhile(_.isDefined).foreach(x => f(x.get))
        }
      }
    })
  }

  def cache(file: File): Coll = cache(new FileOutputStream(file), new FileInputStream(file))

  def cache(file: String): Coll = cache(new File(file))

  /**
    * Cache the entity through an output stream and return the original (not cached) entity.
    *  - if you want to return cached entity, use cache(os, is) or in memory cache()
    *    Streaming action
    *
    * @param os output stream builder
    * @return the same entity
    */
  def cache(os: => OutputStream)(implicit debugger: Debugger): Unit = {
    Serializer.serializeToOutputStream[T](os) { writer =>
      debugger.debug("Dataset caching") { ad =>
        coll.foreach { x =>
          writer.write(x)
          ad.done()
        }
      }
    }
  }

}
