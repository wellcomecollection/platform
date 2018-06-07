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

  // Examples are taken from the MARC spec for field 260.
  // https://www.loc.gov/marc/bibliographic/bd260.html

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

    it("populates dates from subfield c") {
      val production = transform260ToProduction(subfields = List(
        MarcSubfield(tag = "c", content = "1955."),
        MarcSubfield(tag = "c", content = "1984."),
        MarcSubfield(tag = "c", content = "1999.")
      ))

      production.dates shouldBe List(
        Period(label = "1955."),
        Period(label = "1984."),
        Period(label = "1999.")
      )
    }

    it("sets the function as None if it only has subfields a/b/c") {
      val production = transform260ToProduction(subfields = List(
        MarcSubfield(tag = "a", content = "New York"),
        MarcSubfield(tag = "b", content = "Xerox Films"),
        MarcSubfield(tag = "c", content = "1973")
      ))

      production.productionFunction shouldBe None
    }

    it("populates places from a and e, and sets the function as Manufacture") {
      val production = transform260ToProduction(subfields = List(
        MarcSubfield(tag = "a", content = "New York, N.Y."),
        MarcSubfield(tag = "e", content = "Reston, Va."),
        MarcSubfield(tag = "e", content = "[Philadelphia]"),
      ))

      production.places shouldBe List(
        Place(label = "New York, N.Y."),
        Place(label = "Reston, Va."),
        Place(label = "[Philadelphia]")
      )

      production.productionFunction shouldBe Some(Concept("Manufacture"))
    }

    it("populates agents from b and f, and sets the function as Manufacture") {
      val production = transform260ToProduction(subfields = List(
        MarcSubfield(tag = "b", content = "Macmillan"),
        MarcSubfield(tag = "f", content = "Sussex Tapes"),
        MarcSubfield(tag = "f", content = "US Dept of Energy"),
      ))

      production.agents shouldBe List(
        Unidentifiable(Agent(label = "Macmillan")),
        Unidentifiable(Agent(label = "Sussex Tapes")),
        Unidentifiable(Agent(label = "US Dept of Energy"))
      )

      production.productionFunction shouldBe Some(Concept("Manufacture"))
    }

    it("populates dates from c and g, and sets the function as Manufacture") {
      val production = transform260ToProduction(subfields = List(
        MarcSubfield(tag = "c", content = "1981"),
        MarcSubfield(tag = "g", content = "April 15, 1977"),
        MarcSubfield(tag = "g", content = "1973 printing"),
      ))

      production.dates shouldBe List(
        Period(label = "1981"),
        Period(label = "April 15, 1977"),
        Period(label = "1973 printing")
      )

      production.productionFunction shouldBe Some(Concept("Manufacture"))
    }

    it("picks up multiple instances of the 260 field") {
      val varFields = List(
        VarField(
          marcTag = Some("260"),
          fieldTag = "a",
          subfields = List(
            MarcSubfield(tag = "a", content = "London"),
            MarcSubfield(tag = "b", content = "Arts Council of Great Britain"),
            MarcSubfield(tag = "c", content = "1976"),
            MarcSubfield(tag = "e", content = "Twickenham"),
            MarcSubfield(tag = "f", content = "CTD Printers"),
            MarcSubfield(tag = "g", content = "1974")
          )
        ),
        VarField(
          marcTag = Some("260"),
          fieldTag = "a",
          subfields = List(
            MarcSubfield(tag = "a", content = "Bethesda, Md"),
            MarcSubfield(tag = "b", content = "Toxicology Information Program, National Library of Medicine"),
            MarcSubfield(tag = "a", content = "Springfield, Va"),
            MarcSubfield(tag = "b", content = "National Technical Information Service"),
            MarcSubfield(tag = "c", content = "1974-")
          )
        )
      )

      val expectedProductions = List(
        ProductionEvent(
          places = List(Place("London"), Place("Twickenham")),
          agents = List(
            Unidentifiable(Agent("Arts Council of Great Britain")),
            Unidentifiable(Agent("CTD Printers"))
          ),
          dates = List(Period("1976"), Period("1974")),
          productionFunction = Some(Concept("Manufacture"))
        ),
        ProductionEvent(
          places = List(Place("Bethesda, Md"), Place("Springfield, Va")),
          agents = List(
            Unidentifiable(Agent("Toxicology Information Program, National Library of Medicine")),
            Unidentifiable(Agent("National Technical Information Service"))
          ),
          dates = List(Period("1974-")),
          productionFunction = None
        )
      )

      transformToProduction(varFields) shouldBe expectedProductions
    }
  }

  // Examples are taken from the MARC spec for field 264.
  // https://www.loc.gov/marc/bibliographic/bd264.html

  describe("MARC field 264") {
    it("populates places from subfield a") {
      val production = transform264ToProduction(subfields = List(
        MarcSubfield(tag = "a", content = "Boston"),
        MarcSubfield(tag = "a", content = "Cambridge")
      ))

      production.places shouldBe List(
        Place(label = "Boston"),
        Place(label = "Cambridge")
      )
    }

    it("populates agents from subfield b") {
      val production = transform264ToProduction(subfields = List(
        MarcSubfield(tag = "b", content = "ABC Publishers"),
        MarcSubfield(tag = "b", content = "Iverson Company")
      ))

      production.agents shouldBe List(
        Unidentifiable(Agent(label = "ABC Publishers")),
        Unidentifiable(Agent(label = "Iverson Company"))
      )
    }

    it("populates dates from subfield c") {
      val production = transform264ToProduction(subfields = List(
        MarcSubfield(tag = "c", content = "2002"),
        MarcSubfield(tag = "c", content = "1983"),
        MarcSubfield(tag = "c", content = "copyright 2005")
      ))

      production.dates shouldBe List(
        Period(label = "2002"),
        Period(label = "1983"),
        Period(label = "copyright 2005")
      )
    }

    describe("production function") {
      it("sets Production from 2nd indicator == 0") {
        checkProductionFunctionFor264(indicator2 = "0", expectedFunction = "Production")
      }

      it("sets Publication from 2nd indicator == 1") {
        checkProductionFunctionFor264(indicator2 = "1", expectedFunction = "Publication")
      }

      it("sets Distribution from 2nd indicator == 2") {
        checkProductionFunctionFor264(indicator2 = "2", expectedFunction = "Distribution")
      }

      it("sets Manufacture from 2nd indicator == 3") {
        checkProductionFunctionFor264(indicator2 = "3", expectedFunction = "Manufacture")
      }

      it("throws an error if the 2nd indicator is unrecognised") {
        val varFields = List(
          VarField(
            marcTag = Some("264"),
            fieldTag = "a",
            indicator2 = Some("x")
          )
        )

        val caught = intercept[GracefulFailureException] {
          transformToProduction(varFields)
        }

        caught.getMessage shouldBe "Unrecognised second indicator for production function: [Some(x)]"
      }
    }

    it("picks up multiple instances of the 264 field") {
      val varFields = List(
        VarField(
          marcTag = Some("264"),
          fieldTag = "a",
          indicator2 = Some("1"),
          subfields = List(
            MarcSubfield(tag = "a", content = "Columbia, S.C."),
            MarcSubfield(tag = "b", content = "H.W. Williams Co."),
            MarcSubfield(tag = "c", content = "1982")
          )
        ),
        VarField(
          marcTag = Some("264"),
          fieldTag = "a",
          indicator2 = Some("2"),
          subfields = List(
            MarcSubfield(tag = "a", content = "Washington"),
            MarcSubfield(tag = "b", content = "U.S. G.P.O."),
            MarcSubfield(tag = "c", content = "1981-")
          )
        )
      )

      val expectedProductions = List(
        ProductionEvent(
          places = List(Place("Columbia, S.C.")),
          agents = List(Unidentifiable(Agent("H.W. Williams Co."))),
          dates = List(Period("1982")),
          productionFunction = Some(Concept("Publication"))
        ),
        ProductionEvent(
          places = List(Place("Washington")),
          agents = List(Unidentifiable(Agent("U.S. G.P.O."))),
          dates = List(Period("1981-")),
          productionFunction = Some(Concept("Distribution"))
        )
      )

      transformToProduction(varFields) shouldBe expectedProductions
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

  private def transform264ToProduction(subfields: List[MarcSubfield]) = {
    val varFields = List(
      VarField(
        marcTag = Some("264"),
        fieldTag = "a",
        subfields = subfields,
        indicator2 = Some("1")
      )
    )

    transformToProduction(varFields = varFields).head
  }

  private def checkProductionFunctionFor264(indicator2: String, expectedFunction: String) = {
    val varFields = List(
      VarField(
        marcTag = Some("264"),
        fieldTag = "a",
        indicator2 = Some(indicator2)
      )
    )

    val production = transformToProduction(varFields = varFields).head
    production.productionFunction shouldBe Some(Concept(expectedFunction))
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
