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
      contributors shouldBe List(
        Contributor(agent = Unidentifiable(Person(label = name)))
      )
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
      contributors shouldBe List(
        Contributor(agent = Unidentifiable(Person(label = name)))
      )
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
        Contributor(agent = Unidentifiable(Person(label = name1))),
        Contributor(agent = Unidentifiable(Person(label = name2))),
        Contributor(agent = Unidentifiable(Person(label = name3)))
      )
    }

    it("gets the creator prefix from MARC tag 100 subfield $$c") {
      val name = "Darla the Dandelion"
      val prefix = "Dr"

      val bibData = SierraBibData(
        id = "4713698",
        title = None,
        varFields = List(
          VarField(
            fieldTag = "p",
            marcTag = "100",
            indicator1 = "",
            indicator2 = "",
            subfields = List(
              MarcSubfield(tag = "a", content = name),
              MarcSubfield(tag = "c", content = prefix))
          ))
      )

      val contributors = transformer.getContributors(bibData)
      contributors shouldBe List(
        Contributor(agent = Unidentifiable(Person(
          label = name,
          prefix = Some(prefix)
        )))
      )
    }

    it("gets the creator prefix from MARC tag 700 subfield $$c") {
      val name = "Roland the Radish"
      val prefix = "Rev"

      val bibData = SierraBibData(
        id = "6932323",
        title = None,
        varFields = List(
          VarField(
            fieldTag = "p",
            marcTag = "100",
            indicator1 = "",
            indicator2 = "",
            subfields = List(
              MarcSubfield(tag = "a", content = name),
              MarcSubfield(tag = "c", content = prefix))
          ))
      )

      val contributors = transformer.getContributors(bibData)
      contributors shouldBe List(
        Contributor(agent = Unidentifiable(Person(
          label = name,
          prefix = Some(prefix)
        )))
      )
    }

    it("joins multiple instances of subfield $$c into a single prefix") {
      val name = "Mick the Mallow"
      val prefix1 = "Mx"
      val prefix2 = "Mr"

      val bibData = SierraBibData(
        id = "4713698",
        title = None,
        varFields = List(
          VarField(
            fieldTag = "p",
            marcTag = "100",
            indicator1 = "",
            indicator2 = "",
            subfields = List(
              MarcSubfield(tag = "a", content = name),
              MarcSubfield(tag = "c", content = prefix1),
              MarcSubfield(tag = "c", content = prefix2)
            )
          ))
      )

      val contributors = transformer.getContributors(bibData)
      contributors shouldBe List(
        Contributor(agent = Unidentifiable(Person(
          label = name,
          prefix = Some(s"${prefix1} ${prefix2}")
        )))
      )
    }

    it("gets the numeration from subfield $$b") {
      val name = "Leopold the Lettuce"
      val numeration = "LX"

      val bibData = SierraBibData(
        id = "1321010",
        title = None,
        varFields = List(
          VarField(
            fieldTag = "p",
            marcTag = "100",
            indicator1 = "",
            indicator2 = "",
            subfields = List(
              MarcSubfield(tag = "a", content = name),
              MarcSubfield(tag = "b", content = numeration)
            )
          )
        )
      )

      val contributors = transformer.getContributors(bibData)
      contributors shouldBe List(
        Contributor(agent = Unidentifiable(
          Person(label = name, numeration = Some(numeration))
        ))
      )
    }

    it("gets the roles from subfield $$e") {
      val name = "Violet the Vanilla"
      val role1 = "spice"
      val role2 = "flavour"

      val bibData = SierraBibData(
        id = "1661847",
        title = None,
        varFields = List(
          VarField(
            fieldTag = "p",
            marcTag = "100",
            indicator1 = "",
            indicator2 = "",
            subfields = List(
              MarcSubfield(tag = "a", content = name),
              MarcSubfield(tag = "e", content = role1),
              MarcSubfield(tag = "e", content = role2)
            )
          )
        )
      )

      val contributors = transformer.getContributors(bibData)
      contributors shouldBe List(
        Contributor(
          agent = Unidentifiable(Person(label = name)),
          roles = List(ContributionRole(role1), ContributionRole(role2))
        )
      )
    }
  }
}
