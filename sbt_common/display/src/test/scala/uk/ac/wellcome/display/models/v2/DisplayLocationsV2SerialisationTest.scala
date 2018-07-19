package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.WorksIncludes
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksUtil

class DisplayLocationsV2SerialisationTest
    extends FunSpec
    with DisplayV2SerialisationTestBase
    with JsonMapperTestUtil
    with WorksUtil {

  it("serialises a physical location correctly") {
    val physicalLocation = PhysicalLocation(
      locationType = LocationType("sgmed"),
      label = "a stack of slick slimes"
    )

    val work = createIdentifiedWorkWith(
      items = List(createItem(locations = List(physicalLocation)))
    )
    val displayWork =
      DisplayWorkV2(work, includes = WorksIncludes(items = true))

    val actualJson = objectMapper.writeValueAsString(displayWork)
    val expectedJson = s"""
                            |{
                            |  "type": "Work",
                            |  "id": "${work.canonicalId}",
                            |  "title": "${work.title}",
                            |  "contributors": [ ],
                            |  "items": [ ${items(work.items)} ],
                            |  "subjects": [ ],
                            |  "genres": [ ],
                            |  "production": [ ]
                            |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("serialises a digital location correctly") {
    val digitalLocation = DigitalLocation(
      url = "https://wellcomelibrary.org/iiif/b22015085/manifest",
      locationType = LocationType("iiif-image")
    )

    val work = createIdentifiedWorkWith(
      items = List(createItem(locations = List(digitalLocation)))
    )
    val displayWork =
      DisplayWorkV2(work, includes = WorksIncludes(items = true))

    val actualJson = objectMapper.writeValueAsString(displayWork)
    val expectedJson = s"""
                          |{
                          |  "type": "Work",
                          |  "id": "${work.canonicalId}",
                          |  "title": "${work.title}",
                          |  "contributors": [ ],
                          |  "items": [ ${items(work.items)} ],
                          |  "subjects": [ ],
                          |  "genres": [ ],
                          |  "production": [ ]
                          |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("serialises a digital location with a license correctly") {
    val digitalLocation = DigitalLocation(
      url = "https://wellcomelibrary.org/iiif/b22015085/manifest",
      locationType = LocationType("iiif-image"),
      license = Some(License_CC0)
    )

    val work = createIdentifiedWorkWith(
      items = List(createItem(locations = List(digitalLocation)))
    )
    val displayWork =
      DisplayWorkV2(work, includes = WorksIncludes(items = true))

    val actualJson = objectMapper.writeValueAsString(displayWork)
    val expectedJson = s"""
                          |{
                          |  "type": "Work",
                          |  "id": "${work.canonicalId}",
                          |  "title": "${work.title}",
                          |  "contributors": [ ],
                          |  "items": [ ${items(work.items)} ],
                          |  "subjects": [ ],
                          |  "genres": [ ],
                          |  "production": [ ]
                          |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }
}
