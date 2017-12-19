package uk.ac.wellcome.transformer.transformers

import uk.ac.wellcome.models.{MergedSierraRecord, Work}

import scala.util.Try

class SierraTransformableTransformer extends TransformableTransformer[MergedSierraRecord] {
  override def transform(transformable: MergedSierraRecord): Try[Work] = ???
}
