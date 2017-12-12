package uk.ac.wellcome

import java.time.Instant

import com.gu.scanamo.DynamoFormat

package object dynamo {
  implicit val instantLongFormat =
    DynamoFormat.coercedXmap[Instant, Long, IllegalArgumentException](
      Instant.ofEpochSecond
    )(
      _.getEpochSecond
    )
}
