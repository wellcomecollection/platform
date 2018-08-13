package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.WorkType
import uk.ac.wellcome.platform.transformer.source.SierraMaterialType
import uk.ac.wellcome.platform.transformer.utils.SierraDataUtil

class SierraWorkTypeTest extends FunSpec with Matchers with SierraDataUtil {

  val transformer = new SierraWorkType {}

  it("extracts WorkType from bib records") {
    val workTypeId = "a"
    val sierraValue = "Books"

    val bibData = createSierraBibDataWith(
      materialType = Some(
        SierraMaterialType(code = workTypeId)
      )
    )

    val expectedWorkType = WorkType(
      id = workTypeId,
      label = sierraValue
    )

    transformer.getWorkType(bibData = bibData) shouldBe Some(expectedWorkType)
  }

  it("trims whitespace from the materialType code") {
    val bibData = createSierraBibDataWith(
      materialType = Some(
        SierraMaterialType(code = "a  ")
      )
    )

    val expectedWorkType = WorkType(
      id = "a",
      label = "Books"
    )

    transformer.getWorkType(bibData = bibData) shouldBe Some(expectedWorkType)
  }
}
