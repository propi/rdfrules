package eu.easyminer.rdf.utils.extensions

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 15. 3. 2018.
  */
object EitherExtension {

  implicit def autoLeft[A, B](x: A): Either[A, B] = Left(x)

  implicit def autoRight[A, B](x: B): Either[A, B] = Right(x)

}
