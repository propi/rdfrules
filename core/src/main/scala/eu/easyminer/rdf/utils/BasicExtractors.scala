package eu.easyminer.rdf.utils

/**
  * Created by Vaclav Zeman on 12. 7. 2017.
  */
object BasicExtractors {

  object AnyToInt {
    def unapply(s: Any): Option[Int] = try {
      if (s == null)
        None
      else
        Some(s match {
          case x: Int => x
          case x: Short => x.toInt
          case x: Byte => x.toInt
          case x => x.toString.toInt
        })
    } catch {
      case _: java.lang.NumberFormatException => None
    }
  }

  object AnyToDouble {
    def unapply(s: Any): Option[Double] = try {
      if (s == null)
        None
      else
        Some(s match {
          case x: Int => x.toDouble
          case x: Double => x
          case x: Float => x.toDouble
          case x: Long => x.toDouble
          case x: Short => x.toDouble
          case x: Byte => x.toDouble
          case x => x.toString.toDouble
        })
    } catch {
      case _: java.lang.NumberFormatException => None
    }
  }

  object AnyToBoolean {
    def unapply(s: Any): Option[Boolean] = if (s == null) {
      None
    } else {
      s match {
        case "true" => Some(true)
        case "false" => Some(false)
        case "1" => Some(true)
        case "0" => Some(false)
        case x: Int if x == 1 => Some(true)
        case x: Int if x == 0 => Some(false)
        case x: Short if x == 1 => Some(true)
        case x: Short if x == 0 => Some(false)
        case x: Byte if x == 1 => Some(true)
        case x: Byte if x == 0 => Some(false)
        case x: Long if x == 1 => Some(true)
        case x: Long if x == 0 => Some(false)
        case x: Float if x == 1 => Some(true)
        case x: Float if x == 0 => Some(false)
        case x: Double if x == 1 => Some(true)
        case x: Double if x == 0 => Some(false)
        case _ => None
      }
    }
  }

}
