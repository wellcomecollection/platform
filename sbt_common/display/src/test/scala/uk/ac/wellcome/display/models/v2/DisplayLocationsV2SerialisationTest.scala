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

    val work = IdentifiedWork(
      canonicalId = "zm9q6c6h",
      sourceIdentifier = sourceIdentifier,
      version = 1,
      title = "A zoo of zebras doing zumba",
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

}
