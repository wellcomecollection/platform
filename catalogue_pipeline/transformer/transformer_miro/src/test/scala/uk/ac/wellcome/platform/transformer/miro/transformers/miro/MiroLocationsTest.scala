package uk.ac.wellcome.platform.transformer.miro.transformers.miro

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{
  DigitalLocation,
  License_CC0,
  LocationType
}
import uk.ac.wellcome.platform.transformer.miro.source.MiroTransformableData
import uk.ac.wellcome.platform.transformer.miro.transformers.MiroLocations

class MiroLocationsTest extends FunSpec with Matchers {
  val transformer = new MiroLocations {}
  it(
    "extracts the digital location and finds the credit line for an image-specific contributor code") {
    transformer.getLocations(
      miroId = "B0011308",
      miroData = MiroTransformableData(
        creditLine = None,
        sourceCode = Some("FDN"),
        useRestrictions = Some("CC-0")
      )
    ) shouldBe List(
      DigitalLocation(
        "https://iiif.wellcomecollection.org/image/B0011308.jpg/info.json",
        LocationType("iiif-image"),
        Some(License_CC0),
        credit = Some("Ezra Feilden")))
  }
}
