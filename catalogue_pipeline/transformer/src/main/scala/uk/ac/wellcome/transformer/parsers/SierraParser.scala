package uk.ac.wellcome.transformer.parsers

import uk.ac.wellcome.models.transformable.{SierraTransformable, Transformable}
import uk.ac.wellcome.utils.JsonUtil

import scala.util.Try

class SierraParser extends TransformableParser[SierraTransformable] {
  override def readFromRecord(
    transformableAsJson: String): Try[Transformable] =
    JsonUtil.fromJson[SierraTransformable](transformableAsJson)
}
