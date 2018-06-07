package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.{MarcSubfield, SierraBibData, VarField}

class SierraProductionTest extends FunSpec with Matchers {

  it("returns an empty list if neither 260 nor 264 are present") {
    transformToProduction(varFields = List()) shouldBe List()
  }

  it("throws an error if both 260 and 264 are present") {
    transformVarFieldsAndAssertIsError(
      varFields = List(
        VarField(marcTag = Some("260"), fieldTag = "a"),
        VarField(marcTag = Some("264"), fieldTag = "a")
      )
    )
  }

  describe("MARC field 260") {
    it("populates places from subfield a") {
      val production = transform260ToProduction(subfields = List(
        MarcSubfield(tag = "a", content = "Paris"),
        MarcSubfield(tag = "a", content = "London")
      ))

      production.places shouldBe List(
        Place(label = "Paris"),
        Place(label = "London")
      )
    }

    it("populates agents from subfield b") {
      val production = transform260ToProduction(subfields = List(
        MarcSubfield(tag = "b", content = "Gauthier-Villars ;"),
        MarcSubfield(tag = "b", content = "Vogue")
      ))

      production.agents shouldBe List(
        Unidentifiable(Agent(label = "Gauthier-Villars ;")),
        Unidentifiable(Agent(label = "Vogue"))
      )
    }
  }

  // Test helpers

  private def transform260ToProduction(subfields: List[MarcSubfield]) = {
    val varFields = List(
      VarField(
        marcTag = Some("260"),
        fieldTag = "a",
        subfields = subfields
      )
    )

    transformToProduction(varFields = varFields).head
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

  private def transformToProduction(varFields: List[VarField]): List[ProductionEvent[MaybeDisplayable[AbstractAgent]]] = {
    val bibData = SierraBibData(
      id = "p1000001",
      title = Some("Practical production of poisonous panthers"),
      varFields = varFields
    )

    transformer.getProduction(bibData)
  }

  val transformer = new SierraProduction {}
}
