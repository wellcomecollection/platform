package uk.ac.wellcome.display.models.v2

import org.scalatest.{Assertion, FunSpec}
import uk.ac.wellcome.display.json.DisplayJsonUtil._
import uk.ac.wellcome.display.models.V2WorksIncludes
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal._

class DisplayLocationsV2SerialisationTest
    extends FunSpec
    with DisplayV2SerialisationTestBase
    with JsonMapperTestUtil
    with WorksGenerators {

  it("serialises a physical location") {
    val physicalLocation = PhysicalLocation(
      locationType = LocationType("sgmed"),
      label = "a stack of slick slimes"
    )

    val work = createIdentifiedWorkWith(
      items = List(createIdentifiedItemWith(locations = List(physicalLocation)))
    )

    val expectedJson = s"""
                            |{
                            |  "type": "Work",
                            |  "id": "${work.canonicalId}",
                            |  "title": "${work.title}",
                            |  "items": [ ${items(work.items)} ]
                            |}""".stripMargin

    assertWorkMapsToJson(work, expectedJson = expectedJson)
  }

  it("serialises a digital location") {
    val digitalLocation = DigitalLocation(
      url = "https://wellcomelibrary.org/iiif/b22015085/manifest",
      locationType = LocationType("iiif-image")
    )

    val work = createIdentifiedWorkWith(
      items = List(createIdentifiedItemWith(locations = List(digitalLocation)))
    )

    val expectedJson = s"""
                          |{
                          |  "type": "Work",
                          |  "id": "${work.canonicalId}",
                          |  "title": "${work.title}",
                          |  "items": [ ${items(work.items)} ]
                          |}""".stripMargin

    assertWorkMapsToJson(work, expectedJson = expectedJson)
  }

  it("serialises a digital location with a license") {
    val digitalLocation = DigitalLocation(
      url = "https://wellcomelibrary.org/iiif/b22015085/manifest",
      locationType = LocationType("iiif-image"),
      license = Some(License_CC0)
    )

    val work = createIdentifiedWorkWith(
      items = List(createIdentifiedItemWith(locations = List(digitalLocation)))
    )

    val expectedJson = s"""
                          |{
                          |  "type": "Work",
                          |  "id": "${work.canonicalId}",
                          |  "title": "${work.title}",
                          |  "items": [ ${items(work.items)} ]
                          |}""".stripMargin

    assertWorkMapsToJson(work, expectedJson = expectedJson)
  }

  private def assertWorkMapsToJson(
    work: IdentifiedWork,
    expectedJson: String
  ): Assertion =
    assertObjectMapsToJson(
      DisplayWorkV2(work, includes = V2WorksIncludes(items = true)),
      expectedJson = expectedJson
    )
}
