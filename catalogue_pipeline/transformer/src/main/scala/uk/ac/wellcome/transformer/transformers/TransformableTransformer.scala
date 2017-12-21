package uk.ac.wellcome.transformer.transformers

import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.transformable.Transformable

import scala.util.Try

trait TransformableTransformer[+T <: Transformable] {
  protected[this] def transformForType(t: T): Try[Option[Work]]

  def transform(transformable: Transformable): Try[Option[Work]] = Try {
    transformable match {
      case t: T => transformForType(t)
      case _ => throw new RuntimeException(s"$transformable is not of the right type")
    }
  }.flatten
}
