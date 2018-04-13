package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Guice
import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.{
  DisplaySerialisationTestBase,
  WorksIncludes,
  WorksUtil
}
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.models.{IdentifiedItem, IdentifiedWork, PhysicalLocation}
import uk.ac.wellcome.test.utils.JsonTestUtil

class DisplayLocationsV2SerialisationTest
    extends FunSpec
    with DisplaySerialisationTestBase
    with JsonTestUtil
    with WorksUtil {
  val injector = Guice.createInjector(DisplayJacksonModule)

  val objectMapper = injector.getInstance(classOf[ObjectMapper])

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
      DisplayWorkV2(work, includes = WorksIncludes(items = true))

    val actualJson = objectMapper.writeValueAsString(displayWork)
    val expectedJson = s"""
                            |{
                            |  "type": "Work",
                            |  "id": "${work.canonicalId}",
                            |  "title": "${work.title.get}",
                            |  "contributors": [ ],
                            |  "items": [ ${items(work.items)} ],
                            |  "subjects": [ ],
                            |  "genres": [ ],
                            |  "publishers": [],
                            |  "placesOfPublication": [ ]
                            |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

}
