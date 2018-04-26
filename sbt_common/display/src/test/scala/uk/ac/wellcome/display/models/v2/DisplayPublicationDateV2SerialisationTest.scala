package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Guice
import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.{DisplaySerialisationTestBase, WorksUtil}
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.work_model.{IdentifiedWork, Period}

class DisplayPublicationDateV2SerialisationTest
    extends FunSpec
    with DisplaySerialisationTestBase
    with JsonTestUtil
    with WorksUtil {
  val injector = Guice.createInjector(DisplayJacksonModule)

  val objectMapper = injector.getInstance(classOf[ObjectMapper])

  it("omits the publicationDate field if it is empty") {
    val work = IdentifiedWork(
      canonicalId = "arfj5cj4",
      sourceIdentifier = sourceIdentifier,
      title = Some("Asking aging armadillos for another appraisal"),
      publicationDate = None,
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
                            |  "placesOfPublication": [ ]
                            |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("includes the publicationDate field if it is present on the Work") {
    val work = IdentifiedWork(
      canonicalId = "avfpwgrr",
      sourceIdentifier = sourceIdentifier,
      title = Some("Ahoy!  Armoured angelfish are attacking the armada!"),
      publicationDate = Some(Period("1923")),
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
                            |  "publicationDate": ${period(
                            work.publicationDate.get)},
                            |  "placesOfPublication": [ ]
                            |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

}
