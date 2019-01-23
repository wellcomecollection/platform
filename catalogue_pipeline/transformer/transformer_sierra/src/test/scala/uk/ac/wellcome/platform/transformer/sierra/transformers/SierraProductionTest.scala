package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.exceptions.SierraTransformerException
import uk.ac.wellcome.platform.transformer.sierra.source.{
  MarcSubfield,
  VarField
}
import uk.ac.wellcome.platform.transformer.sierra.generators.{
  MarcGenerators,
  SierraDataGenerators
}

class SierraProductionTest
    extends FunSpec
    with Matchers
    with MarcGenerators
    with SierraDataGenerators {

  it("returns an empty list if neither 260 nor 264 are present") {
    transformToProduction(varFields = List()) shouldBe List()
  }

  // Examples are taken from the MARC spec for field 260.
  // https://www.loc.gov/marc/bibliographic/bd260.html

  describe("MARC field 260") {
    it("populates places from subfield a") {
      val production = transform260ToProduction(
        subfields = List(
          MarcSubfield(tag = "a", content = "Paris"),
          MarcSubfield(tag = "a", content = "London")
        ))

      production.places shouldBe List(
        Place(label = "Paris"),
        Place(label = "London")
      )
    }

    it("populates agents from subfield b") {
      val production = transform260ToProduction(
        subfields = List(
          MarcSubfield(tag = "b", content = "Gauthier-Villars ;"),
          MarcSubfield(tag = "b", content = "Vogue")
        ))

      production.agents shouldBe List(
        Unidentifiable(Agent(label = "Gauthier-Villars ;")),
        Unidentifiable(Agent(label = "Vogue"))
      )
    }

    it("populates dates from subfield c") {
      val production = transform260ToProduction(
        subfields = List(
          MarcSubfield(tag = "c", content = "1955"),
          MarcSubfield(tag = "c", content = "1984"),
          MarcSubfield(tag = "c", content = "1999")
        ))

      production.dates shouldBe List(
        Period(label = "1955"),
        Period(label = "1984"),
        Period(label = "1999")
      )
    }

    it("sets the function as None if it only has subfields a/b/c") {
      val production = transform260ToProduction(
        subfields = List(
          MarcSubfield(tag = "a", content = "New York"),
          MarcSubfield(tag = "b", content = "Xerox Films"),
          MarcSubfield(tag = "c", content = "1973")
        ))

      production.function shouldBe None
    }

    it("populates places from a and e, and sets the function as Manufacture") {
      val production = transform260ToProduction(
        subfields = List(
          MarcSubfield(tag = "a", content = "New York, N.Y."),
          MarcSubfield(tag = "e", content = "Reston, Va."),
          MarcSubfield(tag = "e", content = "[Philadelphia]"),
        ))

      production.places shouldBe List(
        Place(label = "New York, N.Y."),
        Place(label = "Reston, Va."),
        Place(label = "[Philadelphia]")
      )

      production.function shouldBe Some(Concept("Manufacture"))
    }

    it("populates agents from b and f, and sets the function as Manufacture") {
      val production = transform260ToProduction(
        subfields = List(
          MarcSubfield(tag = "b", content = "Macmillan"),
          MarcSubfield(tag = "f", content = "Sussex Tapes"),
          MarcSubfield(tag = "f", content = "US Dept of Energy"),
        ))

      production.agents shouldBe List(
        Unidentifiable(Agent(label = "Macmillan")),
        Unidentifiable(Agent(label = "Sussex Tapes")),
        Unidentifiable(Agent(label = "US Dept of Energy"))
      )

      production.function shouldBe Some(Concept("Manufacture"))
    }

    it("populates dates from c and g, and sets the function as Manufacture") {
      val production = transform260ToProduction(
        subfields = List(
          MarcSubfield(tag = "c", content = "1981"),
          MarcSubfield(tag = "g", content = "April 15, 1977"),
          MarcSubfield(tag = "g", content = "1973 printing")
        ))

      production.dates shouldBe List(
        Period(label = "1981"),
        Period(label = "April 15, 1977"),
        Period(label = "1973 printing")
      )

      production.function shouldBe Some(Concept("Manufacture"))
    }

    it("picks up multiple instances of the 260 field") {
      val varFields = List(
        createVarFieldWith(
          marcTag = "260",
          subfields = List(
            MarcSubfield(tag = "a", content = "London :"),
            MarcSubfield(tag = "b", content = "Arts Council of Great Britain,"),
            MarcSubfield(tag = "c", content = "1976;"),
            MarcSubfield(tag = "e", content = "Twickenham :"),
            MarcSubfield(tag = "f", content = "CTD Printers,"),
            MarcSubfield(tag = "g", content = "1974")
          )
        ),
        createVarFieldWith(
          marcTag = "260",
          subfields = List(
            MarcSubfield(tag = "a", content = "Bethesda, Md. :"),
            MarcSubfield(
              tag = "b",
              content =
                "Toxicology Information Program, National Library of Medicine [producer] ;"),
            MarcSubfield(tag = "a", content = "Springfield, Va. :"),
            MarcSubfield(
              tag = "b",
              content = "National Technical Information Service [distributor],"),
            MarcSubfield(tag = "c", content = "1974-")
          )
        )
      )

      val expectedProductions = List(
        ProductionEvent(
          label =
            "London : Arts Council of Great Britain, 1976; Twickenham : CTD Printers, 1974",
          places = List(Place("London"), Place("Twickenham")),
          agents = List(
            Unidentifiable(Agent("Arts Council of Great Britain")),
            Unidentifiable(Agent("CTD Printers"))
          ),
          dates = List(Period("1976;"), Period("1974")),
          function = Some(Concept("Manufacture"))
        ),
        ProductionEvent(
          label =
            "Bethesda, Md. : Toxicology Information Program, National Library of Medicine [producer] ; Springfield, Va. : National Technical Information Service [distributor], 1974-",
          places = List(Place("Bethesda, Md."), Place("Springfield, Va.")),
          agents = List(
            Unidentifiable(Agent(
              "Toxicology Information Program, National Library of Medicine [producer] ;")),
            Unidentifiable(
              Agent("National Technical Information Service [distributor]"))
          ),
          dates = List(Period("1974-")),
          function = None
        )
      )

      transformToProduction(varFields) shouldBe expectedProductions
    }

    it("normalises Place and Period labels") {
      val production = transform260ToProduction(
        subfields = List(
          MarcSubfield(tag = "a", content = "Paris  : "),
          MarcSubfield(tag = "a", content = "London :"),
          MarcSubfield(tag = "c", content = "1984 . "),
          MarcSubfield(tag = "c", content = "1999.")
        ))

      production.places shouldBe List(
        Place(label = "Paris"),
        Place(label = "London")
      )

      production.dates shouldBe List(
        Period(label = "1984"),
        Period(label = "1999")
      )
    }
  }

  // Examples are taken from the MARC spec for field 264.
  // https://www.loc.gov/marc/bibliographic/bd264.html

  describe("MARC field 264") {
    it("populates places from subfield a") {
      val production = transform264ToProduction(
        subfields = List(
          MarcSubfield(tag = "a", content = "Boston"),
          MarcSubfield(tag = "a", content = "Cambridge")
        ))

      production.places shouldBe List(
        Place(label = "Boston"),
        Place(label = "Cambridge")
      )
    }

    it("populates agents from subfield b") {
      val production = transform264ToProduction(
        subfields = List(
          MarcSubfield(tag = "b", content = "ABC Publishers"),
          MarcSubfield(tag = "b", content = "Iverson Company")
        ))

      production.agents shouldBe List(
        Unidentifiable(Agent(label = "ABC Publishers")),
        Unidentifiable(Agent(label = "Iverson Company"))
      )
    }

    it("populates dates from subfield c") {
      val production = transform264ToProduction(
        subfields = List(
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
        checkProductionFunctionFor264(
          indicator2 = "0",
          expectedFunction = "Production")
      }

      it("sets Publication from 2nd indicator == 1") {
        checkProductionFunctionFor264(
          indicator2 = "1",
          expectedFunction = "Publication")
      }

      it("sets Distribution from 2nd indicator == 2") {
        checkProductionFunctionFor264(
          indicator2 = "2",
          expectedFunction = "Distribution")
      }

      it("sets Manufacture from 2nd indicator == 3") {
        checkProductionFunctionFor264(
          indicator2 = "3",
          expectedFunction = "Manufacture")
      }

      it("throws an error if the 2nd indicator is unrecognised") {
        val varFields = List(
          createVarFieldWith(
            marcTag = "264",
            indicator2 = "x",
            subfields = List()
          )
        )

        val caught = intercept[SierraTransformerException] {
          transformToProduction(varFields)
        }

        caught.e.getMessage shouldBe "Unrecognised second indicator for production function: [Some(x)]"
      }
    }

    it(
      "ignores instances of the 264 field related to copyright (2nd indicator 4)") {
      val varFields = List(
        createVarFieldWith(
          marcTag = "264",
          indicator2 = "4",
          subfields = List(
            MarcSubfield(tag = "c", content = "copyright 2005")
          )
        ),
        createVarFieldWith(
          marcTag = "264",
          indicator2 = "3",
          subfields = List(
            MarcSubfield(tag = "a", content = "Cambridge :"),
            MarcSubfield(tag = "b", content = "Kinsey Printing Company")
          )
        )
      )

      val expectedProductions = List(
        ProductionEvent(
          label = "Cambridge : Kinsey Printing Company",
          places = List(Place("Cambridge")),
          agents = List(Unidentifiable(Agent("Kinsey Printing Company"))),
          dates = List(),
          function = Some(Concept("Manufacture"))
        )
      )

      transformToProduction(varFields) shouldBe expectedProductions
    }

    it("ignores instances of the 264 field with an empty 2nd indicator") {
      val varFields = List(
        createVarFieldWith(
          marcTag = "264",
          indicator2 = " ",
          subfields = List(
            MarcSubfield(tag = "c", content = "copyright 2005")
          )
        ),
        createVarFieldWith(
          marcTag = "264",
          indicator2 = "3",
          subfields = List(
            MarcSubfield(tag = "a", content = "London :"),
            MarcSubfield(tag = "b", content = "Wellcome Collection Publishing")
          )
        )
      )

      val expectedProductions = List(
        ProductionEvent(
          label = "London : Wellcome Collection Publishing",
          places = List(Place("London")),
          agents = List(Unidentifiable(Agent("Wellcome Collection Publishing"))),
          dates = List(),
          function = Some(Concept("Manufacture"))
        )
      )

      transformToProduction(varFields) shouldBe expectedProductions
    }

    it("picks up multiple instances of the 264 field") {
      val varFields = List(
        createVarFieldWith(
          marcTag = "264",
          indicator2 = "1",
          subfields = List(
            MarcSubfield(tag = "a", content = "Columbia, S.C. :"),
            MarcSubfield(tag = "b", content = "H.W. Williams Co.,"),
            MarcSubfield(tag = "c", content = "1982")
          )
        ),
        createVarFieldWith(
          marcTag = "264",
          indicator2 = "2",
          subfields = List(
            MarcSubfield(tag = "a", content = "Washington :"),
            MarcSubfield(tag = "b", content = "U.S. G.P.O.,"),
            MarcSubfield(tag = "c", content = "1981-")
          )
        )
      )

      val expectedProductions = List(
        ProductionEvent(
          label = "Columbia, S.C. : H.W. Williams Co., 1982",
          places = List(Place("Columbia, S.C.")),
          agents = List(Unidentifiable(Agent("H.W. Williams Co."))),
          dates = List(Period("1982")),
          function = Some(Concept("Publication"))
        ),
        ProductionEvent(
          label = "Washington : U.S. G.P.O., 1981-",
          places = List(Place("Washington")),
          agents = List(Unidentifiable(Agent("U.S. G.P.O."))),
          dates = List(Period("1981-")),
          function = Some(Concept("Distribution"))
        )
      )

      transformToProduction(varFields) shouldBe expectedProductions
    }

    it("normalises Place and Period labels") {
      val production = transform264ToProduction(
        subfields = List(
          MarcSubfield(tag = "a", content = "Boston:"),
          MarcSubfield(tag = "a", content = "Cambridge : "),
          MarcSubfield(tag = "b", content = "ABC Publishers,"),
          MarcSubfield(tag = "b", content = "Iverson Ltd. , "),
          MarcSubfield(tag = "c", content = "2002."),
          MarcSubfield(tag = "c", content = "1983 ."),
          MarcSubfield(tag = "c", content = "copyright 2005.")
        ))

      production.places shouldBe List(
        Place(label = "Boston"),
        Place(label = "Cambridge")
      )
      production.agents shouldBe List(
        Unidentifiable(Agent(label = "ABC Publishers")),
        Unidentifiable(Agent(label = "Iverson Ltd."))
      )
      production.dates shouldBe List(
        Period(label = "2002"),
        Period(label = "1983"),
        Period(label = "copyright 2005")
      )
    }
  }

  describe("Both MARC field 260 and 264") {
    it("throws an error if both 260 and 264 are present") {
      transformVarFieldsAndAssertIsError(
        varFields = List(
          createVarFieldWith(
            marcTag = "260",
            subfields = List(
              MarcSubfield(tag = "a", content = "Paris")
            )
          ),
          createVarFieldWith(
            marcTag = "264",
            subfields = List(
              MarcSubfield(tag = "a", content = "London")
            )
          )
        )
      )
    }

    it(
      "uses field 260 if field 264 only contains a copyright statement in subfield c") {
      val varFields = List(
        createVarFieldWith(
          marcTag = "260",
          subfields = List(
            MarcSubfield(tag = "a", content = "San Francisco :"),
            MarcSubfield(tag = "b", content = "Morgan Kaufmann Publishers,"),
            MarcSubfield(tag = "c", content = "2004")
          )
        ),
        createVarFieldWith(
          marcTag = "264",
          subfields = List(
            MarcSubfield(tag = "c", content = "©2004")
          )
        )
      )

      val expectedProductions = List(
        ProductionEvent(
          label = "San Francisco : Morgan Kaufmann Publishers, 2004",
          places = List(Place("San Francisco")),
          agents = List(
            Unidentifiable(Agent("Morgan Kaufmann Publishers"))
          ),
          dates = List(Period("2004")),
          function = None
        )
      )

      transformToProduction(varFields) shouldBe expectedProductions
    }

    it("returns correctly if 260 and 264 contain the same subfields") {
      val subfields = List(
        MarcSubfield(tag = "a", content = "London :"),
        MarcSubfield(tag = "b", content = "Wellcome Trust,"),
        MarcSubfield(tag = "c", content = "1992")
      )

      val varFields = List(
        createVarFieldWith(
          marcTag = "260",
          subfields = subfields
        ),
        createVarFieldWith(
          marcTag = "264",
          subfields = subfields
        )
      )

      val expectedProductions = List(
        ProductionEvent(
          label = "London : Wellcome Trust, 1992",
          places = List(Place("London")),
          agents = List(
            Unidentifiable(Agent("Wellcome Trust"))
          ),
          dates = List(Period("1992")),
          function = None
        )
      )

      transformToProduction(varFields) shouldBe expectedProductions
    }
  }

  // Test helpers

  private def transform260ToProduction(subfields: List[MarcSubfield]) = {
    val varFields = List(
      createVarFieldWith(
        marcTag = "260",
        subfields = subfields
      )
    )

    transformToProduction(varFields = varFields).head
  }

  private def transform264ToProduction(subfields: List[MarcSubfield]) = {
    val varFields = List(
      createVarFieldWith(
        marcTag = "264",
        indicator2 = "1",
        subfields = subfields
      )
    )

    transformToProduction(varFields = varFields).head
  }

  private def checkProductionFunctionFor264(indicator2: String,
                                            expectedFunction: String) = {
    val varFields = List(
      createVarFieldWith(
        marcTag = "264",
        indicator2 = indicator2,
        subfields = List()
      )
    )

    val production = transformToProduction(varFields = varFields).head
    production.function shouldBe Some(Concept(expectedFunction))
  }

  private def transformVarFieldsAndAssertIsError(varFields: List[VarField]) = {
    val bibData = createSierraBibDataWith(varFields = varFields)

    intercept[SierraTransformerException] {
      transformer.getProduction(bibData)
    }
  }

  private def transformToProduction(varFields: List[VarField])
    : List[ProductionEvent[MaybeDisplayable[AbstractAgent]]] = {
    val bibData = createSierraBibDataWith(varFields = varFields)
    transformer.getProduction(bibData)
  }

  val transformer = new SierraProduction {}
}
