package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{Agent, Organisation, Unidentifiable}
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData, VarField}
import uk.ac.wellcome.test.utils.SierraData

class SierraPublishersTest extends FunSpec with Matchers with SierraData {

  it("picks up zero publishers") {
    assertFindsCorrectPublishers(
      varFields = List(),
      expectedPublisherNames = List()
    )
  }

  it("ignores subfields unrelated to the name of the publisher") {
    assertFindsCorrectPublishers(
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "260",
          indicator1 = " ",
          indicator2 = " ",
          subfields = List(
            MarcSubfield(tag = "c", content = "1984")
          )
        )
      ),
      expectedPublisherNames = List()
    )
  }

  it("picks up information about the name of the publisher") {
    assertFindsCorrectPublishers(
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "260",
          indicator1 = " ",
          indicator2 = " ",
          subfields = List(
            MarcSubfield(tag = "b", content = "Peaceful Poetry")
          )
        )
      ),
      expectedPublisherNames = List("Peaceful Poetry")
    )
  }

  it("picks up information about multiple publishers") {
    // Based on an example in
    // http://www.loc.gov/marc/bibliographic/bd260.html
    assertFindsCorrectPublishers(
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "260",
          indicator1 = " ",
          indicator2 = " ",
          subfields = List(
            MarcSubfield(tag = "a", content = "Paris"),
            MarcSubfield(tag = "b", content = "Gauthier-Villars"),
            MarcSubfield(tag = "a", content = "Chicago"),
            MarcSubfield(tag = "b", content = "University of Chicago Press"),
            MarcSubfield(tag = "c", content = "1955")
          )
        )
      ),
      expectedPublisherNames = List(
        "Gauthier-Villars",
        "University of Chicago Press"
      )
    )
  }

  it("uses MARC field 264 if field 260 is not present") {
    assertFindsCorrectPublishers(
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "264",
          indicator1 = " ",
          indicator2 = " ",
          subfields = List(
            MarcSubfield(tag = "b", content = "Daring Diaries")
          )
        )
      ),
      expectedPublisherNames = List("Daring Diaries")
    )
  }

  it("ignores MARC field 264 if field 260 is present") {
    assertFindsCorrectPublishers(
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "260",
          indicator1 = " ",
          indicator2 = " ",
          subfields = List(
            MarcSubfield(tag = "b", content = "Harrowing Hardbacks")
          )
        ),
        VarField(
          fieldTag = "p",
          marcTag = "264",
          indicator1 = " ",
          indicator2 = " ",
          subfields = List(
            MarcSubfield(tag = "b", content = "Nail-Biting Novels")
          )
        )
      ),
      expectedPublisherNames = List("Harrowing Hardbacks")
    )
  }

  it("picks up multiple instances of MARC field 264 if necessary") {
    assertFindsCorrectPublishers(
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "264",
          indicator1 = " ",
          indicator2 = " ",
          subfields = List(
            MarcSubfield(tag = "b", content = "Brilliant Books")
          )
        ),
        VarField(
          fieldTag = "p",
          marcTag = "264",
          indicator1 = " ",
          indicator2 = " ",
          subfields = List(
            MarcSubfield(tag = "b", content = "Thrilling Tomes"),
            MarcSubfield(tag = "b", content = "Page-Turning Paperbacks")
          )
        )
      ),
      expectedPublisherNames = List(
        "Brilliant Books",
        "Thrilling Tomes",
        "Page-Turning Paperbacks"
      )
    )
  }

  val transformer = new Object with SierraPublishers

  private def assertFindsCorrectPublishers(
    varFields: List[VarField],
    expectedPublisherNames: List[String]
  ) = {

    val bibData = SierraBibData(
      id = "b1234567",
      title = Some("A pack of published puffins in Paris"),
      deleted = false,
      suppressed = false,
      varFields = varFields
    )

    val expectedPublishers = expectedPublisherNames.map { name =>
      Unidentifiable(Organisation(label = name))
    }

    transformer.getPublishers(bibData = bibData) shouldBe expectedPublishers
  }
}
