package uk.ac.wellcome.transformer.transformers
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.transformable.miro.MiroTransformable

import scala.util.Try

class MiroTransformableTransformer extends TransformableTransformer[MiroTransformable] {
  override def transform(transformable: MiroTransformable): Try[Work] = ???
}
