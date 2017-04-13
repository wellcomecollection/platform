package uk.ac.wellcome.transformer.parsers

import com.gu.scanamo.ScanamoFree
import com.gu.scanamo.error.DynamoReadError
import com.twitter.inject.Logging
import uk.ac.wellcome.models.CalmTransformable
import uk.ac.wellcome.transformer.receive.RecordMap

class CalmParser extends TransformableParser[CalmTransformable] with Logging {
  override def readFromRecord(
    recordMap: RecordMap): Either[DynamoReadError, CalmTransformable] =
    ScanamoFree.read[CalmTransformable](recordMap.value)
}
