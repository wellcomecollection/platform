package uk.ac.wellcome.transformer.transformers

import uk.ac.wellcome.models.{CalmTransformable, Work}

import scala.util.Try

class CalmTransformableTransformer extends TransformableTransformer[CalmTransformable] {
  override def transform(transformable: CalmTransformable): Try[Option[Work]] = ???
}
