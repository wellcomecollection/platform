package uk.ac.wellcome.platform.sierra_items_to_dynamo

import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.syntax._
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibNumber,
  SierraItemNumber,
  SierraItemRecord
}
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.type_classes.IdGetter

package object dynamo {
  implicit val bibNumberFormat: DynamoFormat[SierraBibNumber] =
    DynamoFormat
      .coercedXmap[SierraBibNumber, String, IllegalArgumentException](
        SierraBibNumber
      )(
        _.withoutCheckDigit
      )

  implicit val itemNumberFormat: DynamoFormat[SierraItemNumber] =
    DynamoFormat
      .coercedXmap[SierraItemNumber, String, IllegalArgumentException](
        SierraItemNumber
      )(
        _.withoutCheckDigit
      )

  implicit val idGetter: IdGetter[SierraItemRecord] =
    (record: SierraItemRecord) => record.id.withoutCheckDigit

  implicit val updateExpressionGenerator
    : UpdateExpressionGenerator[SierraItemRecord] =
    (record: SierraItemRecord) =>
      Some(
        set('bibIds -> record.bibIds) and
          set('data -> record.data) and
          set('modifiedDate -> record.modifiedDate) and
          set('unlinkedBibIds -> record.unlinkedBibIds)
    )
}
