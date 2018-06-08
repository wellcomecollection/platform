package uk.ac.wellcome.platform.transformer.transformers.miro

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}
import uk.ac.wellcome.platform.transformer.source.MiroTransformableData

class MiroIdentifiersTest extends FunSpec with Matchers {

  it("fixes the malformed INNOPAC ID on L0035411") {
    val miroData = MiroTransformableData(
      innopacID = Some("L 35411 \n\n15551040")
    )

    val identifiers =
      transformer.getIdentifiers(miroData = miroData, miroId = "L0035411")

    identifiers shouldBe List(
      SourceIdentifier(
        identifierType = IdentifierType("miro-image-number"),
        value = "L0035411",
        ontologyType = "Work"
      ),
      SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        value = "b15551040",
        ontologyType = "Work"
      )
    )
  }

  val transformer = new MiroIdentifiers {}
}
