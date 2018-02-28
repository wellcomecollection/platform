package uk.ac.wellcome

import java.time.Instant

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.DynamoReadError
import shapeless.{the, HList, Lazy}

package object dynamo {
  implicit val instantLongFormat =
    DynamoFormat.coercedXmap[Instant, String, IllegalArgumentException](
      Instant.parse
    )(
      _.toString
    )

  // DynamoFormat for tagged HLists
  implicit def hlistDynamoFormat[T <: HList](
    implicit formatR: Lazy[DynamoFormat.ValidConstructedDynamoFormat[T]]) =
    new DynamoFormat[T] {
      def read(av: AttributeValue): Either[DynamoReadError, T] =
        formatR.value.read(av).toEither
      def write(t: T): AttributeValue = formatR.value.write(t)
    }
}
