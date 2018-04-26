package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Guice
import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.{DisplaySerialisationTestBase, WorksUtil}
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.work_model.{IdentifiedWork, Place}

class DisplayPlaceOfPublicationV2SerialisationTest
    extends FunSpec
    with DisplaySerialisationTestBase
    with JsonTestUtil
    with WorksUtil {
  val injector = Guice.createInjector(DisplayJacksonModule)

  val objectMapper = injector.getInstance(classOf[ObjectMapper])

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
