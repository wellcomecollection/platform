package uk.ac.wellcome.transformer.transformers

import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.transformable.Transformable

import scala.util.Try

trait TransformableTransformer[+T <: Transformable] {
  def transform(transformable: Transformable): Try[Option[Work]]
}
