package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.WorkType
import uk.ac.wellcome.transformer.source.{SierraBibData, SierraMaterialType}

class SierraWorkTypeTest extends FunSpec with Matchers {

  val transformer = new SierraWorkType {}

  it("should extract WorkType from bib records") {

    val workTypeId = "workTypeCode"
    val sierraValue = "Sierra Material Type Label"

    val bibData = SierraBibData(
      id = "b1234567",
      title = Some("A trifle of tangy tangerine tigers"),
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
