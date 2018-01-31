package uk.ac.wellcome.transformer.parsers

import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.utils.JsonUtil._
import scala.util.Try

class TransformableParser extends Logging {
  final def extractTransformable(message: String): Try[Transformable] =
    fromJson[Transformable](message)
      .recover {
        case e: Throwable =>
          error("Error extracting Transformable case class", e)
          throw e
      }
}
