package uk.ac.wellcome.transformer.transformers

import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.transformable.Transformable

import scala.util.Try

trait TransformableTransformer[+T <: Transformable] {
  protected[this] def transform(transformable: T): Try[Work]
}
