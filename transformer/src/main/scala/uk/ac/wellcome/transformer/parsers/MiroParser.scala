package uk.ac.wellcome.transformer.parsers

import com.gu.scanamo.ScanamoFree
import com.gu.scanamo.error.DynamoReadError
import com.twitter.inject.Logging
import uk.ac.wellcome.models.MiroTransformable
import uk.ac.wellcome.transformer.receive.RecordMap

class MiroParser extends TransformableParser[MiroTransformable] with Logging {
  override def readFromRecord(
    recordMap: RecordMap): Either[DynamoReadError, MiroTransformable] =
    ScanamoFree.read[MiroTransformable](recordMap.value)
}
