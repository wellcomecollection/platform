package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.platform.transformer.miro.generators.MiroRecordGenerators

class MiroIdentifiersTest
    extends FunSpec
    with Matchers
    with IdentifiersGenerators
    with MiroRecordGenerators {

  it("fixes the malformed INNOPAC ID on L0035411") {
    val miroRecord = createMiroRecordWith(
      innopacID = Some("L 35411 \n\n15551040"),
      imageNumber = "L0035411"
    )

    val otherIdentifiers =
      transformer.getOtherIdentifiers(miroRecord = miroRecord)

    otherIdentifiers shouldBe List(
      createSierraSystemSourceIdentifierWith(
        value = "b15551040"
      )
    )
  }

  val transformer = new MiroIdentifiers {}
}
