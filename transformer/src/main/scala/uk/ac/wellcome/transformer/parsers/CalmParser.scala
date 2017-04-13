package uk.ac.wellcome.transformer.parsers

import com.gu.scanamo.ScanamoFree
import com.twitter.inject.Logging
import uk.ac.wellcome.models.{CalmTransformable, Transformable}
import uk.ac.wellcome.transformer.receive.RecordMap

import scala.util.Try

class CalmParser extends TransformableParser with Logging {
  override def extractTransformable(recordMap: RecordMap): Try[Transformable] =
    Try { ScanamoFree.read[CalmTransformable](recordMap.value) }.map {
    case Right(calmDynamoRecord) =>
      info(s"Parsed DynamoDB record $calmDynamoRecord")
      calmDynamoRecord
    case Left(dynamoReadError) =>
      error(s"Unable to parse record ${recordMap.value}")
      throw new Exception(
        s"Unable to parse record ${recordMap.value} received $dynamoReadError")
  }.recover {
    case e: Throwable =>
      error("Error extracting transformable case class", e)
      throw e
  }
}
