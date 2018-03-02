package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{Agent, IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.test.utils.SierraData
import uk.ac.wellcome.transformer.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}

class SierraPhysicalDescriptionTest extends FunSpec with Matchers {

  val transformer = new SierraPhysicalDescription {}

  it("gets no physical if there is no MARC field 300 with subfield $c") {
    val bibData = SierraBibData(
      id = "pd1000001",
      title = Some("Quick!  The quails are quadrupling."),
      varFields = List()
    )

    transformer.getPhysicalDescription(bibData = bibData).get shouldBe None
  }

  it("extracts physical description from MARC field 300 subfield $b") {
    val physicalDescription = "Queuing quokkas quarrel about Quirinus Quirrell"

    val varFields = List(
      VarField(
        fieldTag = "?",
        marcTag = "300",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List(
          MarcSubfield(
            tag = "a",
            content = "The edifying extent of early emus"
          ),
          MarcSubfield(
            tag = "b",
            content = physicalDescription
          )
        )
      )
    )

    val bibData = SierraBibData(
      id = "pd2000002",
      title = Some("Quaint quants are quite quiet"),
      varFields = varFields
    )

    transformer.getPhysicalDescription(bibData = bibData).get shouldBe physicalDescription
  }

  it("extracts a physical description where there are multiple MARC field 399") {
    val physicalDescription1 = "The queer quolls quits and quarrels"
    val physicalDescription2 = "A quintessential quadraped is quick"

    val expectedPhysicalDescription = s"$physicalDescription1\n\n$physicalDescription2"

    varFields = List(
      VarField(
        fieldTag = "?",
        marcTag = "300",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List(
          MarcSubfield(
            tag = "b",
            content = physicalDescription1
          )
        )
      ),
      VarField(
        fieldTag = "?",
        marcTag = "300",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List(
          MarcSubfield(
            tag = "a",
            content = "Egad!  An early eagle is eating the earwig."
          ),
          MarcSubfield(
            tag = "b",
            content = physicalDescription2
          )
        )
      )
    )

    val bibData = SierraBibData(
      id = "pd3000003",
      title = Some("A qualified quetzal is quixotic"),
      varFields = varFields
    )

    transformer.getPhysicalDescription(bibData = bibData).get shouldBe expectedPhysicalDescription
  }
}
