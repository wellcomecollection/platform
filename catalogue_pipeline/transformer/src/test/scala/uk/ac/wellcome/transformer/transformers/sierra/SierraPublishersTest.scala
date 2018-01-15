package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.Agent
import uk.ac.wellcome.transformer.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}
import uk.ac.wellcome.test.utils.SierraData

// import java.time.Instant.now
//
//
// import uk.ac.wellcome.models._
// import uk.ac.wellcome.models.transformable.SierraTransformable
// import uk.ac.wellcome.models.transformable.sierra.{
//   SierraBibRecord,
//   SierraItemRecord
// }
//

class SierraPublishersTest
    extends FunSpec
    with Matchers
    with SierraData {

  it("picks up zero publishers") {
    assertFindsCorrectPublishers(
      varFields = List(),
      expectedPublishers = List()
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
      expectedPublishers = List()
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
            MarcSubfield(tag = "b", content = "H. Humphrey")
          )
        )
      ),
      expectedPublishers = List(
        Agent(
          label = "H. Humphrey",
          ontologyType = "Organisation"
        ))
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
      expectedPublishers = List(
        Agent(
          label = "Gauthier-Villars",
          ontologyType = "Organisation"
        ),
        Agent(
          label = "University of Chicago Press",
          ontologyType = "Organisation"
        )
      )
    )
  }

  val transformer = new Object with SierraPublishers

  private def assertFindsCorrectPublishers(
    varFields: List[VarField],
    expectedPublishers: List[Agent]
  ) = {

    val bibData = SierraBibData(
      id = "b1234567",
      title = "A pack of published puffins in Paris",
      deleted = false,
      suppressed = false,
      varFields = varFields
    )

    transformer.getPublishers(bibData = bibData) shouldBe expectedPublishers
  }
}
