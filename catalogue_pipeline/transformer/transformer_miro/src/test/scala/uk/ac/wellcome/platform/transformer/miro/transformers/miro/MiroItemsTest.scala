package uk.ac.wellcome.platform.transformer.miro.transformers.miro

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.miro.source.MiroTransformableData
import uk.ac.wellcome.platform.transformer.miro.transformers.MiroItems

class MiroItemsTest extends FunSpec with Matchers with IdentifiersGenerators {
  val transformer = new MiroItems {}

  describe("getItemsV1") {
    it("extracts an identifiable item") {
      transformer.getItemsV1(
        miroId = "B0011308",
        miroData = MiroTransformableData(
          creditLine = None,
          sourceCode = Some("FDN"),
          useRestrictions = Some("CC-0")
        )) shouldBe List(
        Identifiable(
          agent = Item(locations = List(DigitalLocation(
            "https://iiif.wellcomecollection.org/image/B0011308.jpg/info.json",
            LocationType("iiif-image"),
            Some(License_CC0),
            credit = Some("Ezra Feilden")))),
          sourceIdentifier = createMiroSourceIdentifierWith(
            value = "B0011308",
            ontologyType = "Item"
          )
        ))
    }
  }
  describe("getItems") {
    it("extracts an unidentifiable item") {
      transformer.getItems(
        miroId = "B0011308",
        miroData = MiroTransformableData(
          creditLine = None,
          sourceCode = Some("FDN"),
          useRestrictions = Some("CC-0")
        )) shouldBe List(
        Unidentifiable(
          agent = Item(locations = List(DigitalLocation(
            "https://iiif.wellcomecollection.org/image/B0011308.jpg/info.json",
            LocationType("iiif-image"),
            Some(License_CC0),
            credit = Some("Ezra Feilden"))))))
    }
  }
}
