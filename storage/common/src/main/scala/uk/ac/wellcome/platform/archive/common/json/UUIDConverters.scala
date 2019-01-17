package uk.ac.wellcome.platform.archive.common.json

import java.util.UUID

import com.gu.scanamo.DynamoFormat
import uk.ac.wellcome.json.JsonUtil._

trait UUIDConverters {
  implicit val fmtUuid =
    DynamoFormat.coercedXmap[UUID, String, IllegalArgumentException](
      fromJson[UUID](_).get
    )(
      toJson[UUID](_).get
    )
}
