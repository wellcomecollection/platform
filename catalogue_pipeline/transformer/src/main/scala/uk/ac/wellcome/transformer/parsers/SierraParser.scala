package uk.ac.wellcome.transformer.parsers

import uk.ac.wellcome.models.MergedSierraRecord
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.utils.JsonUtil

import scala.util.Try

class SierraParser extends TransformableParser[MergedSierraRecord] {
  override def readFromRecord(
    transformableAsJson: String): Try[Transformable] =
    JsonUtil.fromJson[MergedSierraRecord](transformableAsJson)
}
