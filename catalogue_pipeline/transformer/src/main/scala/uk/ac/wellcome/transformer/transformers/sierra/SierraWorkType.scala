package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.transformer.source.SierraBibData
import uk.ac.wellcome.work_model.WorkType

trait SierraWorkType {

  /* Populate wwork:workType. Rules:
   *
   * 1. For all bibliographic records use "materialType"
   * 2. Platform "id" is populated from "code"
   * 3. Platform "label" is populated from "value"
   *
   * Example:
   *  "workType": {
   *     "id": "e-book",
   *     "type": "WorkType",
   *     "label": "E-books"
   *     },
   *
   * Note: will map to a controlled vocabulary terms in future
   */
  def getWorkType(bibData: SierraBibData): Option[WorkType] =
    bibData.materialType.map { t =>
      WorkType(id = t.code, label = t.value)
    }
}
