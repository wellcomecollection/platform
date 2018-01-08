package uk.ac.wellcome.transformer.parsers

import com.twitter.inject.Logging
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.utils.JsonUtil

import scala.util.Try

class MiroParser extends TransformableParser[MiroTransformable] with Logging {
  override def readFromRecord(
    transformableAsJson: String): Try[MiroTransformable] =
    JsonUtil.fromJson[MiroTransformable](transformableAsJson)
}
