package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
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
    val displayWork =
      DisplayWorkV2(work, includes = V2WorksIncludes(items = true))

    val actualJson = objectMapper.writeValueAsString(displayWork)
    val expectedJson = s"""
                            |{
                            |  "type": "Work",
                            |  "id": "${work.canonicalId}",
                            |  "title": "${work.title}",
                            |  "items": [ ${items(work.items)} ]
                            |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("serialises a digital location") {
    val digitalLocation = DigitalLocation(
      url = "https://wellcomelibrary.org/iiif/b22015085/manifest",
      locationType = LocationType("iiif-image")
    )

    val work = createIdentifiedWorkWith(
      items = List(createIdentifiedItemWith(locations = List(digitalLocation)))
    )
    val displayWork =
      DisplayWorkV2(work, includes = V2WorksIncludes(items = true))

    val actualJson = objectMapper.writeValueAsString(displayWork)
    val expectedJson = s"""
                          |{
                          |  "type": "Work",
                          |  "id": "${work.canonicalId}",
                          |  "title": "${work.title}",
                          |  "items": [ ${items(work.items)} ]
                          |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
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
    val displayWork =
      DisplayWorkV2(work, includes = V2WorksIncludes(items = true))

    val actualJson = objectMapper.writeValueAsString(displayWork)
    val expectedJson = s"""
                          |{
                          |  "type": "Work",
                          |  "id": "${work.canonicalId}",
                          |  "title": "${work.title}",
                          |  "items": [ ${items(work.items)} ]
                          |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }
}
