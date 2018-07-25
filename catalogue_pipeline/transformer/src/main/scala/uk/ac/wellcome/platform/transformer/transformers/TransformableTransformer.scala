package uk.ac.wellcome.platform.transformer.transformers

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.models.work.internal.TransformedBaseWork

import scala.util.{Failure, Try}

trait TransformableTransformer[T <: Transformable] extends Logging {
  def transformForType
    : PartialFunction[(Transformable, Int), Try[TransformedBaseWork]]

  def transform(transformable: Transformable,
                version: Int): Try[TransformedBaseWork] =
    transformable match {
      case t if transformForType.isDefinedAt((t, version)) =>
        transformForType((t, version))
      case _ =>
        Failure(
          new RuntimeException(s"$transformable is not of the right type"))
    }
}
