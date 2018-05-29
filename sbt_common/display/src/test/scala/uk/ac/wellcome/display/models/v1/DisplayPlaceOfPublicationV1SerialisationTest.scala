package uk.ac.wellcome.display.models.v1

import org.scalatest.FunSpec
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal.{IdentifiedWork, Place}
import uk.ac.wellcome.models.work.test.util.WorksUtil

class DisplayPlaceOfPublicationV1SerialisationTest
    extends FunSpec
    with DisplayV1SerialisationTestBase
    with JsonMapperTestUtil
    with WorksUtil {

  it("serialises the placesOfPublication field") {
    val work = IdentifiedWork(
      canonicalId = "avfpwgrr",
      sourceIdentifier = sourceIdentifier,
      title = Some("Ahoy!  Armoured angelfish are attacking the armada!"),
      placesOfPublication = List(Place("Durmstrang")),
      version = 1
    )
    val displayWork = DisplayWorkV1(work)

    val actualJson = objectMapper.writeValueAsString(displayWork)
    val expectedJson = s"""
                            |{
                            |  "type": "Work",
                            |  "id": "${work.canonicalId}",
                            |  "title": "${work.title.get}",
                            |  "creators": [ ],
                            |  "subjects": [ ],
                            |  "genres": [ ],
                            |  "publishers": [],
                            |  "placesOfPublication": [
                            |    {
                            |      "label": "${work.placesOfPublication.head.label}",
                            |      "type": "Place"
                            |    }
                            |  ]
                            |
                             |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

}
