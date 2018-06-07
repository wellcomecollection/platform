package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.work.internal.{
  AbstractAgent,
  MaybeDisplayable,
  ProductionEvent
}
import uk.ac.wellcome.platform.transformer.source.{SierraBibData, VarField}

class SierraProductionTest extends FunSpec with Matchers {

  it("returns an empty list if neither 260 nor 264 are present") {
    transformVarFieldsAndAssertProductionIsCorrect(
      varFields = List(),
      expectedProduction = List()
    )
  }

  it("throws an error if both 260 and 264 are present") {
    transformVarFieldsAndAssertIsError(
      varFields = List(
        VarField(marcTag = Some("260"), fieldTag = "a"),
        VarField(marcTag = Some("264"), fieldTag = "a")
      )
    )
  }

  private def transformVarFieldsAndAssertIsError(varFields: List[VarField]) = {
    val bibData = SierraBibData(
      id = "p1000001",
      title = Some("Practical production of poisonous panthers"),
      varFields = varFields
    )

    intercept[GracefulFailureException] {
      transformer.getProduction(bibData)
    }
  }

  private def transformVarFieldsAndAssertProductionIsCorrect(
    varFields: List[VarField],
    expectedProduction: List[ProductionEvent[MaybeDisplayable[AbstractAgent]]]
  ) = {
    val bibData = SierraBibData(
      id = "p1000001",
      title = Some("Practical production of poisonous panthers"),
      varFields = varFields
    )

    transformer.getProduction(bibData) shouldBe expectedProduction
  }

  val transformer = new SierraProduction {}
}
