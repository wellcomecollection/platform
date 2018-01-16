package uk.ac.wellcome.transformer.parsers

import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.circe.jsonUtil._
import scala.util.Try

class TransformableParser extends Logging {
  final def extractTransformable(message: SQSMessage): Try[Transformable] =
    fromJson[Transformable](message.body)
      .recover {
        case e: Throwable =>
          error("Error extracting Transformable case class", e)
          throw e
      }
}
