package uk.ac.wellcome.transformer.parsers

import com.gu.scanamo.error.DynamoReadError
import com.twitter.inject.Logging
import uk.ac.wellcome.models.Transformable
import uk.ac.wellcome.transformer.receive.RecordMap

import scala.util.Try

trait TransformableParser[+T <: Transformable] extends Logging {
  final def extractTransformable(recordMap: RecordMap): Try[Transformable] =
    Try { readFromRecord(recordMap) }
      .map {
        case Right(transformable) =>
          info(s"Parsed DynamoDB record $transformable")
          transformable
        case Left(dynamoReadError) =>
          error(s"Unable to parse record ${recordMap.value}")
          throw new Exception(
            s"Unable to parse record ${recordMap.value} received $dynamoReadError")
      }
      .recover {
        case e: Throwable =>
          error("Error extracting Transformable case class", e)
          throw e
      }

  def readFromRecord(recordMap: RecordMap): Either[DynamoReadError, T]
}
