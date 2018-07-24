package uk.ac.wellcome.sierra_adapter.test.utils

import java.time.Instant

import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.sierra_adapter.models.SierraRecord

trait SierraRecordUtil extends SierraUtil {
  def createSierraRecordWith(id: SierraRecordNumber,
                             data: String,
                             modifiedDate: String): SierraRecord =
    createSierraRecordWith(
      id = id,
      data = data,
      modifiedDate = Instant.parse(modifiedDate)
    )

  def createSierraRecordWith(id: String): SierraRecord =
    createSierraRecordWith(
      id = SierraRecordNumber(id)
    )

  def createSierraRecordWith(
    id: SierraRecordNumber = createSierraRecordNumber,
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
