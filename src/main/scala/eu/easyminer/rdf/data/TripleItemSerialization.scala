package eu.easyminer.rdf.data

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import eu.easyminer.rdf.utils.serialization.{Deserializer, Serializer}

/**
  * Created by Vaclav Zeman on 5. 10. 2017.
  */
object TripleItemSerialization {

  private def tripleItemToIndex(tripleItem: TripleItem): Int = tripleItem match {
    case _: TripleItem.LongUri => 1
    case _: TripleItem.PrefixedUri => 2
    case _: TripleItem.BlankNode => 3
    case _: TripleItem.Text => 4
    case TripleItem.Number(_: Int) => 5
    case TripleItem.Number(_: Double) => 6
    case TripleItem.Number(_: Float) => 7
    case TripleItem.Number(_: Long) => 8
    case TripleItem.Number(_: Short) => 9
    case TripleItem.Number(_: Byte) => 10
    case _: TripleItem.BooleanValue => 11
  }

  /**
    * First block is byte: type of triple item
    * Others are value bytes
    */
  implicit val tripleItemSerializer: Serializer[TripleItem] = (v: TripleItem) => {
    val baos = new ByteArrayOutputStream()
    baos.write(tripleItemToIndex(v))
    v match {
      case TripleItem.LongUri(uri) => baos.write(Serializer.serialize(uri))
      case TripleItem.PrefixedUri(prefix, nameSpace, localName) =>
        baos.write(Serializer.serialize(prefix))
        baos.write(Serializer.serialize(nameSpace))
        baos.write(Serializer.serialize(localName))
      case TripleItem.BlankNode(id) => baos.write(Serializer.serialize(id))
      case TripleItem.Text(value) => baos.write(Serializer.serialize(value))
      case TripleItem.Number(x: Int) => baos.write(Serializer.serialize(x))
      case TripleItem.Number(x: Double) => baos.write(Serializer.serialize(x))
      case TripleItem.Number(x: Float) => baos.write(Serializer.serialize(x))
      case TripleItem.Number(x: Long) => baos.write(Serializer.serialize(x))
      case TripleItem.Number(x: Short) => baos.write(Serializer.serialize(x))
      case TripleItem.Number(x: Byte) => baos.write(Serializer.serialize(x))
      case TripleItem.BooleanValue(x) => baos.write(Serializer.serialize(x))
      case x => throw new Serializer.SerializationException("No serializer for triple item: " + x.getClass.getName)
    }
    baos.toByteArray
  }

  implicit val tripleItemDeserializer: Deserializer[TripleItem] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    bais.read() match {
      case 1 => TripleItem.LongUri(Deserializer.deserialize[String](bais).get)
      case 2 =>
        val prefix = Deserializer.deserialize[String](bais).get
        val nameSpace = Deserializer.deserialize[String](bais).get
        val localName = Deserializer.deserialize[String](bais).get
        TripleItem.PrefixedUri(prefix, nameSpace, localName)
      case 3 => TripleItem.BlankNode(Deserializer.deserialize[String](bais).get)
      case 4 => TripleItem.Text(Deserializer.deserialize[String](bais).get)
      case 5 => TripleItem.Number(Deserializer.deserialize[Int](bais).get)
      case 6 => TripleItem.Number(Deserializer.deserialize[Double](bais).get)
      case 7 => TripleItem.Number(Deserializer.deserialize[Float](bais).get)
      case 8 => TripleItem.Number(Deserializer.deserialize[Long](bais).get)
      case 9 => TripleItem.Number(Deserializer.deserialize[Short](bais).get)
      case 10 => TripleItem.Number(Deserializer.deserialize[Byte](bais).get)
      case 11 => TripleItem.BooleanValue(Deserializer.deserialize[Boolean](bais).get)
      case x => throw new Deserializer.DeserializationException("No deserializer for index: " + x)
    }
  }

  implicit val tripleItemUriSerializer: Serializer[TripleItem.Uri] = (v: TripleItem.Uri) => tripleItemSerializer.serialize(v)

  implicit val tripleItemUriDeserializer: Deserializer[TripleItem.Uri] = (v: Array[Byte]) => tripleItemDeserializer.deserialize(v) match {
    case x: TripleItem.Uri => x
    case x => throw new Deserializer.DeserializationException("Deserialization error, TripleItem.Uri expected instead of " + x.getClass.getName)
  }

}
