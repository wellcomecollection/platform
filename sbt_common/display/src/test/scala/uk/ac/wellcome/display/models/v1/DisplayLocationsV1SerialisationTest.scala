package uk.ac.wellcome.display.models.v1

import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.{
  DisplaySerialisationTestBase,
  WorksIncludes
}
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal.{
  IdentifiedItem,
  IdentifiedWork,
  PhysicalLocation
}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksUtil

class DisplayLocationsV1SerialisationTest
    extends FunSpec
    with DisplaySerialisationTestBase
    with JsonMapperTestUtil
    with WorksUtil {

  it("serialises a physical location correctly") {
    val physicalLocation = PhysicalLocation(
      locationType = "smeg",
      label = "a stack of slick slimes"
    )

    val work = IdentifiedWork(
      canonicalId = "zm9q6c6h",
      sourceIdentifier = sourceIdentifier,
      version = 1,
      title = Some("A zoo of zebras doing zumba"),
      items = List(
        IdentifiedItem(
          canonicalId = "mhberjwy7",
          sourceIdentifier = sourceIdentifier,
          locations = List(physicalLocation)
        )
      )
    )
    val displayWork =
      DisplayWorkV1(work, includes = WorksIncludes(items = true))

    val actualJson = objectMapper.writeValueAsString(displayWork)
    val expectedJson = s"""
                            |{
                            |  "type": "Work",
                            |  "id": "${work.canonicalId}",
                            |  "title": "${work.title.get}",
                            |  "creators": [ ],
                            |  "items": [ ${items(work.items)} ],
                            |  "subjects": [ ],
                            |  "genres": [ ],
                            |  "publishers": [],
                            |  "placesOfPublication": [ ]
                            |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

}
