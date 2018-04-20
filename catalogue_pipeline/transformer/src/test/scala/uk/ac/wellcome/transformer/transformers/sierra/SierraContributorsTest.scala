package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData, VarField}

class SierraContributorsTest extends FunSpec with Matchers {

  val transformer = new SierraContributors {}

  it("gets an empty contributor list from empty bib data") {
    val bibData = SierraBibData(
      id = "3224766",
      title = None,
      varFields = List()
    )

    val contributors = transformer.getContributors(bibData)
    contributors shouldBe List()
  }

  describe("Person") {
    it("gets the name from MARC tag 100 subfield $$a") {
      val name = "Carol the Carrot"

      val bibData = SierraBibData(
        id = "7344264",
        title = None,
        varFields = List(
          VarField(
            fieldTag = "p",
            marcTag = "100",
            indicator1 = "",
            indicator2 = "",
            subfields = List(MarcSubfield(tag = "a", content = name))))
      )

      val contributors = transformer.getContributors(bibData)
      contributors shouldBe List(Unidentifiable(Person(label = name)))
    }

    it("gets the name from MARC tag 700 subfield $$a") {
      val name = "Bertrand the Beetroot"

      val bibData = SierraBibData(
        id = "4530272",
        title = None,
        varFields = List(
          VarField(
            fieldTag = "p",
            marcTag = "700",
            indicator1 = "",
            indicator2 = "",
            subfields = List(MarcSubfield(tag = "a", content = name))))
      )

      val contributors = transformer.getContributors(bibData)
      contributors shouldBe List(Unidentifiable(Person(label = name)))
    }

    it("gets the name from MARC tags 100 and 700 subfield $$a in the right order") {
      val name1 = "Alfie the Artichoke"
      val name2 = "Alison the Apple"
      val name3 = "Archie the Aubergine"

      // The correct ordering is "everything from 100 first, then 700", and
      // we deliberately pick an ordering that's different from that for
      // the MARC fields, so we can check it really is applying this rule.
      val bibData = SierraBibData(
        id = "8261371",
        title = None,
        varFields = List(
          VarField(
            fieldTag = "p",
            marcTag = "700",
            indicator1 = "",
            indicator2 = "",
            subfields = List(MarcSubfield(tag = "a", content = name2))
          ),
          VarField(
            fieldTag = "p",
            marcTag = "100",
            indicator1 = "",
            indicator2 = "",
            subfields = List(MarcSubfield(tag = "a", content = name1))
          ),
          VarField(
            fieldTag = "p",
            marcTag = "700",
            indicator1 = "",
            indicator2 = "",
            subfields = List(MarcSubfield(tag = "a", content = name3))
          )
        )
      )

      val contributors = transformer.getContributors(bibData)
      contributors shouldBe List(
        Unidentifiable(Person(label = name1)),
        Unidentifiable(Person(label = name2))
      )
    }
  }
}
