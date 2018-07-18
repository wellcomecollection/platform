package uk.ac.wellcome.sierra_adapter.test.util

import java.time.Instant

import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.sierra_adapter.models.SierraRecord

trait SierraRecordUtil extends SierraUtil {
  def createSierraRecordWith(id: String = createSierraRecordNumberString): SierraRecord =
    SierraRecord(
      id = SierraRecordNumber(id),
      data = "{}",
      modifiedDate = Instant.now
    )

  def createSierraRecord: SierraRecord =
    createSierraRecordWith()
}
