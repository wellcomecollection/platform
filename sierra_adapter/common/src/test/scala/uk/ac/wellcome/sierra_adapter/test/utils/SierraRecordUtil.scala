package uk.ac.wellcome.sierra_adapter.test.utils

import java.time.Instant

import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.sierra_adapter.models.SierraRecord

trait SierraRecordUtil extends SierraUtil {
  def createSierraRecordWith(id: String,
                             data: String,
                             modifiedDate: String): SierraRecord =
    createSierraRecordWith(
      id = id,
      data = data,
      modifiedDate = Instant.parse(modifiedDate)
    )

  def createSierraRecordWith(
    id: String = createSierraRecordNumberString,
    data: String = "",
    modifiedDate: Instant = Instant.now): SierraRecord = {
    val recordData = if (data == "") {
      s"""{"id": "$id"}"""
    } else data

    SierraRecord(
      id = id,
      data = recordData,
      modifiedDate = modifiedDate
    )
  }
}
