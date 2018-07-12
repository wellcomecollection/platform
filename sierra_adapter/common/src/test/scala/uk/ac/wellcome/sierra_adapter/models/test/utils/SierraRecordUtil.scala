package uk.ac.wellcome.sierra_adapter.models.test.utils

import java.time.Instant

import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.sierra_adapter.models.SierraRecord

trait SierraRecordUtil extends SierraUtil {
  def createSierraRecordWith(
    id: String = createSierraId,
    data: String = "<<default data>>"
  ): SierraRecord =
    SierraRecord(
      id = id,
      data = data,
      modifiedDate = Instant.now
    )
}
