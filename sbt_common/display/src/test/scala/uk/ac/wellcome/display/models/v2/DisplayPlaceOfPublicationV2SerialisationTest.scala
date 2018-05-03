package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.display.test.util.{JsonMapperTestUtil, WorksUtil}
import uk.ac.wellcome.models.work.internal.{IdentifiedWork, Place}

class DisplayPlaceOfPublicationV2SerialisationTest
    extends FunSpec
    with DisplaySerialisationTestBase
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
    val displayWork = DisplayWorkV2(work)

    val actualJson = objectMapper.writeValueAsString(displayWork)
    val expectedJson = s"""
                            |{
                            |  "type": "Work",
                            |  "id": "${work.canonicalId}",
                            |  "title": "${work.title.get}",
                            |  "contributors": [ ],
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
