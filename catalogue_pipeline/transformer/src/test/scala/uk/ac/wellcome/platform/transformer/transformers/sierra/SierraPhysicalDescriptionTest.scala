package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.transformer.source.{MarcSubfield, VarField}
import uk.ac.wellcome.platform.transformer.utils.SierraDataUtil

class SierraPhysicalDescriptionTest extends FunSpec with Matchers with SierraDataUtil {

  val transformer = new SierraPhysicalDescription {}

  it(
    "gets no physical description if there is no MARC field 300 with subfield $b") {
    val bibData = createSierraBibDataWith(varFields = List())
    transformer.getPhysicalDescription(bibData = bibData) shouldBe None
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

    val bibData = createSierraBibDataWith(varFields = varFields)

    transformer
      .getPhysicalDescription(bibData = bibData)
      .get shouldBe physicalDescription
  }

  it(
    "extracts a physical description where there are multiple MARC field 300 $b") {
    val physicalDescription1 = "The queer quolls quits and quarrels"
    val physicalDescription2 = "A quintessential quadraped is quick"

    val expectedPhysicalDescription =
      s"$physicalDescription1\n\n$physicalDescription2"

    val varFields = List(
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

    val bibData = createSierraBibDataWith(varFields = varFields)

    transformer
      .getPhysicalDescription(bibData = bibData)
      .get shouldBe expectedPhysicalDescription
  }
}
