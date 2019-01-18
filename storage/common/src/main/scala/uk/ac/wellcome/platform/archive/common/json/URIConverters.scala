package uk.ac.wellcome.platform.archive.common.json

import java.net.URI

import com.gu.scanamo.DynamoFormat
import uk.ac.wellcome.json.JsonUtil._

trait URIConverters {
  implicit val fmtUri =
    DynamoFormat.coercedXmap[URI, String, IllegalArgumentException](
      fromJson[URI](_).get
    )(
      toJson[URI](_).get
    )
}
