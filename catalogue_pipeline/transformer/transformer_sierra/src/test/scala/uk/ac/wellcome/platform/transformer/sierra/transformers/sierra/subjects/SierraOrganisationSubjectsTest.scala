package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra.subjects

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.generators.SierraDataGenerators
import uk.ac.wellcome.platform.transformer.sierra.source.{MarcSubfield, SierraBibData, VarField}

class SierraOrganisationSubjectsTest extends FunSpec with Matchers with SierraDataGenerators {
  it("returns an empty list if there are no instances of MARC tag 610") {
    val bibData = createSierraBibDataWith(varFields = List())
    transformer.getSubjectsWithOrganisation(bibData) shouldBe List()
  }

  describe("label") {
    it("uses subfields a, b, c, d and e as the label") {
      val bibData = create610bibDataWith(
        subfields = List(
          MarcSubfield(tag = "a", content = "United States."),
          MarcSubfield(tag = "b", content = "Supreme Court,"),
          MarcSubfield(tag = "c", content = "Washington, DC."),
          MarcSubfield(tag = "d", content = "September 29, 2005,"),
          MarcSubfield(tag = "e", content = "pictured.")
        )
      )

      val subjects = transformer.getSubjectsWithOrganisation(bibData)
      subjects should have size 1

      subjects.head.label shouldBe "United States. Supreme Court, Washington, DC. September 29, 2005, pictured."
    }

    it("uses repeated subfields for the label if necessary") {
      // This is based on an example from the MARC spec
      val bibData = create610bibDataWith(
        subfields = List(
          MarcSubfield(tag = "a", content = "United States."),
          MarcSubfield(tag = "b", content = "Army."),
          MarcSubfield(tag = "b", content = "Cavalry, 7th."),
          MarcSubfield(tag = "b", content = "Company E,"),
          MarcSubfield(tag = "e", content = "depicted.")
        )
      )

      val subjects = transformer.getSubjectsWithOrganisation(bibData)
      subjects should have size 1

      subjects.head.label shouldBe "United States. Army. Cavalry, 7th. Company E, depicted."
    }
  }

  describe("concepts") {
    it("creates an Organisation as the concept") {
      val bibData = create610bibDataWith(
        subfields = List(
          MarcSubfield(tag = "a", content = "Wellcome Trust."),
        )
      )

      val subjects = transformer.getSubjectsWithOrganisation(bibData)
      val concepts = subjects.head.concepts
      concepts should have size 1

      val maybeDisplayableOrganisation = concepts.head
      maybeDisplayableOrganisation shouldBe a[Unidentifiable[_]]
    }

    it("uses subfields a and b for the Organisation label") {
      val bibData = create610bibDataWith(
        subfields = List(
          MarcSubfield(tag = "a", content = "Wellcome Trust."),
          MarcSubfield(tag = "b", content = "Facilities,"),
          MarcSubfield(tag = "b", content = "Health & Safety"),
          MarcSubfield(tag = "c", content = "27 September 2018")
        )
      )

      val subjects = transformer.getSubjectsWithOrganisation(bibData)
      val concepts = subjects.head.concepts
      val organisation = concepts.head.agent
      organisation.label shouldBe "Wellcome Trust. Facilities, Health & Safety"
    }

    it("creates an Identifiable Organisation if subfield 0 is present") {
      val lcNamesCode = "n81290903210"
      val bibData = create610bibDataWith(
        subfields = List(
          MarcSubfield(tag = "a", content = "ACME Corp"),
          MarcSubfield(tag = "0", content = lcNamesCode)
        )
      )

      val subjects = transformer.getSubjectsWithOrganisation(bibData)
      val concepts = subjects.head.concepts
      val maybeDisplayableOrganisation = concepts.head
      maybeDisplayableOrganisation shouldBe a[Identifiable[_]]

      val identifiableOrganisation = maybeDisplayableOrganisation.asInstanceOf[Identifiable[Organisation]]
      identifiableOrganisation.identifiers shouldBe List(
        SourceIdentifier(
          identifierType = IdentifierType("lc-names"),
          ontologyType = "Organisation",
          value = lcNamesCode
        )
      )
    }

    it("skips adding an identifier if subfield 0 is ambiguous") {
      val bibData = create610bibDataWith(
        subfields = List(
          MarcSubfield(tag = "a", content = "ACME Corp"),
          MarcSubfield(tag = "0", content = "n12345"),
          MarcSubfield(tag = "0", content = "n67890")
        )
      )

      val subjects = transformer.getSubjectsWithOrganisation(bibData)
      val concepts = subjects.head.concepts
      val maybeDisplayableOrganisation = concepts.head
      maybeDisplayableOrganisation shouldBe a[Unidentifiable[_]]
    }
  }

  val transformer = new SierraOrganisationSubjects {}

  private def create610bibDataWith(subfields: List[MarcSubfield]): SierraBibData =
    createSierraBibDataWith(
      varFields = List(
        createMarc610VarField(subfields = subfields)
      )
    )

  private def createMarc610VarField(subfields: List[MarcSubfield]): VarField =
    VarField(
      marcTag = "610",
      indicator1 = "",
      indicator2 = "",
      subfields = subfields
    )
}
