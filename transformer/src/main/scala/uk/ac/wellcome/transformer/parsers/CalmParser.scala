package uk.ac.wellcome.transformer.parsers

import com.twitter.inject.Logging
import uk.ac.wellcome.models.CalmTransformable
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.utils.JsonUtil

import scala.util.Try

class CalmParser extends TransformableParser[CalmTransformable] with Logging {
  override def readFromRecord(message: SQSMessage): Try[CalmTransformable] =
    JsonUtil.fromJson[CalmTransformable](message.body)
}
