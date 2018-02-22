package uk.ac.wellcome.transformer.transformers

import uk.ac.wellcome.models.UnidentifiedWork
import uk.ac.wellcome.models.transformable.Transformable

import scala.util.Try

trait TransformableTransformer[+T <: Transformable] {
  protected[this] def transformForType(
    t: T,
    version: Int): Try[Option[UnidentifiedWork]]

  def transform(transformable: Transformable,
                version: Int): Try[Option[UnidentifiedWork]] =
    Try {
      transformable match {
        case t: T => transformForType(t, version)
        case _ =>
          throw new RuntimeException(
            s"$transformable is not of the right type")
      }
    }.flatten
}
