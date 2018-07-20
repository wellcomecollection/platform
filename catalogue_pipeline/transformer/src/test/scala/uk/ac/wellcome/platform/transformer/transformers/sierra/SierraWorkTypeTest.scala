package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.WorkType
import uk.ac.wellcome.platform.transformer.source.SierraMaterialType
import uk.ac.wellcome.platform.transformer.utils.SierraDataUtil

class SierraWorkTypeTest extends FunSpec with Matchers with SierraDataUtil {

  val transformer = new SierraWorkType {}

  it("should extract WorkType from bib records") {

    val workTypeId = "workTypeCode"
    val sierraValue = "Sierra Material Type Label"

    val bibData = createSierraBibDataWith(
      materialType = Some(
        SierraMaterialType(
          code = workTypeId,
          value = sierraValue
        ))
    )

    val expectedWorkType = WorkType(
      id = workTypeId,
      label = sierraValue
    )

    transformer.getWorkType(bibData = bibData) shouldBe Some(expectedWorkType)
  }
}
