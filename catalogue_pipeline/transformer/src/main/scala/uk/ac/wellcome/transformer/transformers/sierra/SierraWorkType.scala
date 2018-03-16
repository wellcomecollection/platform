package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.WorkType
import uk.ac.wellcome.transformer.source.SierraBibData

trait SierraWorkType {
  def getWorkType(bibData: SierraBibData): Option[WorkType] =
    bibData.materialType.map { t =>
      WorkType(id = t.code, label = t.value)
    }
}
