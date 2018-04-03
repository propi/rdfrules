package com.github.propi.rdfrules.serialization

trait RuleSerialization {

  /*implicit private def atomItemToInt(item: Atom.Item): Int = item match {
    case x: Atom.Variable => x.index
    case x: Atom.Constant => x.value
  }

  object AtomSerialization {

    /**
      * Bytes:
      * - 4: predicate int
      * - 1: true/false - is subject variable
      * - 4: subject int
      * - 1: true/false - is object variable
      * - 4: object int
      */
    implicit val atomSerializationSize: SerializationSize[Atom] = new SerializationSize[Atom] {
      val size: Int = 14
    }

    implicit val atomSerializer: Serializer[Atom] = (v: Atom) => {
      ByteBuffer.allocate(atomSerializationSize.size)
        .put(Serializer.serialize(v.predicate))
        .put(Serializer.serialize(v.subject.isInstanceOf[Atom.Variable]))
        .put(Serializer.serialize[Int](v.subject))
        .put(Serializer.serialize(v.`object`.isInstanceOf[Atom.Variable]))
        .put(Serializer.serialize[Int](v.`object`))
        .array()
    }

    implicit val atomDeserializer: Deserializer[Atom] = (v: Array[Byte]) => {
      val bais = new ByteArrayInputStream(v)
      val predicate = Deserializer.deserialize[Int](bais)
      val subject: Atom.Item = if (Deserializer.deserialize[Boolean](bais)) Atom.Variable(Deserializer.deserialize[Int](bais)) else Atom.Constant(Deserializer.deserialize[Int](bais))
      val `object`: Atom.Item = if (Deserializer.deserialize[Boolean](bais)) Atom.Variable(Deserializer.deserialize[Int](bais)) else Atom.Constant(Deserializer.deserialize[Int](bais))
      Atom(subject, predicate, `object`)
    }

  }

  object MeasuresSerialization {

    /**
      * Bytes:
      * - 4: int - head size
      * - 4: int - support
      * - 8: double - head coverage
      * - 4: int - body size
      * - 8: double - confidence
      * - 8: double - pca confidence
      * - 8: double - lift
      * - 8: double - pca lift
      */
    implicit val measuresSerializationSize: SerializationSize[Measures] = new SerializationSize[Measures] {
      val size: Int = 52
    }

    implicit val measuresSerializer: Serializer[Measures] = (v: Measures) => ByteBuffer.allocate(measuresSerializationSize.size)
      .put(Serializer.serialize(v[Measure.HeadSize].value))
      .put(Serializer.serialize(v[Measure.Support].value))
      .put(Serializer.serialize(v[Measure.HeadCoverage].value))
      .put(Serializer.serialize(v.get[Measure.BodySize].map(_.value).getOrElse(-1)))
      .put(Serializer.serialize(v.get[Measure.Confidence].map(_.value).getOrElse(-1.0)))
      .put(Serializer.serialize(v.get[Measure.PcaConfidence].map(_.value).getOrElse(-1.0)))
      .put(Serializer.serialize(v.get[Measure.Lift].map(_.value).getOrElse(-1.0)))
      .put(Serializer.serialize(v.get[Measure.PcaLift].map(_.value).getOrElse(-1.0)))
      .array()

    implicit val measuresDeserializer: Deserializer[Measures] = (v: Array[Byte]) => {
      val bais = new ByteArrayInputStream(v)
      val m = Measures(
        Measure.HeadSize(Deserializer.deserialize[Int](bais)),
        Measure.Support(Deserializer.deserialize[Int](bais)),
        Measure.HeadCoverage(Deserializer.deserialize[Double](bais))
      )

      def addIf[T <: Measure](measure: T)(f: T => Boolean): Unit = if (f(measure)) m += measure

      addIf(Measure.BodySize(Deserializer.deserialize[Int](bais)))(_.value >= 0)
      addIf(Measure.Confidence(Deserializer.deserialize[Double](bais)))(_.value >= 0)
      addIf(Measure.PcaConfidence(Deserializer.deserialize[Double](bais)))(_.value >= 0)
      addIf(Measure.Lift(Deserializer.deserialize[Double](bais)))(_.value >= 0)
      addIf(Measure.PcaLift(Deserializer.deserialize[Double](bais)))(_.value >= 0)
      m
    }

  }

  implicit val ruleSerializer: Serializer[Rule] = (v: Rule) => {
    import AtomSerialization._
    val measuresBytes = Serializer.serialize(v.measures)
    val atomsBytes = Serializer.serialize((v.head +: v.body).asInstanceOf[Traversable[Atom]])
    measuresBytes ++ atomsBytes
  }

  implicit val ruleDeserializer: Deserializer[Rule] = (v: Array[Byte]) => {
    import AtomSerialization._
    import MeasuresSerialization._
    val bais = new ByteArrayInputStream(v)
    val measures = Deserializer.deserialize[Measures](bais)
    val atoms = Deserializer.deserialize[Traversable[Atom]](bais)
    Rule.Simple(atoms.head, atoms.tail.toIndexedSeq)(measures)
  }*/

}