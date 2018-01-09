package uk.ac.wellcome.transformer.parsers

import com.twitter.inject.Logging
import io.circe.parser.decode
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.Transformable
import scala.util.{Failure, Success, Try}


class TransformableParser extends Logging {
  final def extractTransformable(message: SQSMessage): Try[Transformable] =
    readFromRecord(message.body)
      .recover {
        case e: Throwable =>
          error("Error extracting Transformable case class", e)
          throw e
      }

  def readFromRecord(message: String): Try[Transformable] = {
    decode[Transformable](message) match {
      case Right(transformable) => Success(transformable)
      case Left(parseError) => Failure(parseError)
    }
  }
}