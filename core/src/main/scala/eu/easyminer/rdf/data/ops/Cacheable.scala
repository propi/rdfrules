package eu.easyminer.rdf.data.ops

import java.io.{InputStream, OutputStream}

import eu.easyminer.rdf.utils.serialization.{Deserializer, SerializationSize, Serializer}

/**
  * Created by Vaclav Zeman on 27. 2. 2018.
  */
trait Cacheable[T, Coll] extends Transformable[T, Coll] {

  self: Coll =>

  protected implicit val serializer: Serializer[T]
  protected implicit val deserializer: Deserializer[T]
  protected implicit val serializationSize: SerializationSize[T]

  def cache: Coll = transform(coll.toList)

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

  def cache(os: => OutputStream): Coll = {
    Serializer.serializeToOutputStream[T](os) { writer =>
      coll.foreach(writer.write)
    }
    this
  }

}
