package uk.ac.wellcome.transformer.parsers

import com.twitter.inject.Logging
import uk.ac.wellcome.models.MiroTransformable
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.utils.JsonUtil

import scala.util.Try

class MiroParser extends TransformableParser[MiroTransformable] with Logging {
  override def readFromRecord(message: SQSMessage): Try[MiroTransformable] =
    JsonUtil.fromJson[MiroTransformable](message.body)
}
