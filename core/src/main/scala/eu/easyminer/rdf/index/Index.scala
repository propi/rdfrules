package eu.easyminer.rdf.index

import java.io.{InputStream, OutputStream}

import eu.easyminer.rdf.data.Dataset
import eu.easyminer.rdf.utils.serialization.{Deserializer, SerializationSize, Serializer}

import scala.collection.TraversableView

/**
  * Created by Vaclav Zeman on 4. 10. 2017.
  */
trait Index[T] {

  def save(buildOutputStream: => OutputStream)(f: Serializer.Writer[T] => Unit)(implicit serializer: Serializer[T], serializationSize: SerializationSize[T]): Unit = {
    Serializer.serializeToOutputStream[T](buildOutputStream)(f)
  }

  def save(items: Traversable[T], buildOutputStream: => OutputStream)(implicit serializer: Serializer[T], serializationSize: SerializationSize[T]): Unit = {
    save(buildOutputStream) { writer =>
      items.foreach(writer.write)
    }
  }

  def load[R](buildInputStream: => InputStream)(f: Deserializer.Reader[T] => R)(implicit deserializer: Deserializer[T], serializationSize: SerializationSize[T]): R = {
    Deserializer.deserializeFromInputStream[T, R](buildInputStream)(f)
  }

  def loadToTraversable(buildInputStream: => InputStream)(implicit deserializer: Deserializer[T], serializationSize: SerializationSize[T]): TraversableView[T, Traversable[T]] = {
    new Traversable[T] {
      def foreach[U](f: (T) => U): Unit = load(buildInputStream)(reader => Stream.continually(reader.read()).takeWhile(_.nonEmpty).map(_.get).foreach(f))
    }.view
  }

}
