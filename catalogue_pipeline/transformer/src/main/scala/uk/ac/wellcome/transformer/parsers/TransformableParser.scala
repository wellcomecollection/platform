package uk.ac.wellcome.transformer.parsers

import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.Transformable

import scala.util.Try

trait TransformableParser[+T <: Transformable] extends Logging {
  final def extractTransformable(message: SQSMessage): Try[Transformable] =
    readFromRecord(message.body)
      .recover {
        case e: Throwable =>
          error("Error extracting Transformable case class", e)
          throw e
      }

  def readFromRecord(message: String): Try[Transformable]
}
