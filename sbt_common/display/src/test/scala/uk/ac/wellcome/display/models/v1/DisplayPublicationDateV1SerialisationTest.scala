package uk.ac.wellcome.display.models.v1

import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal.{IdentifiedWork, Period}
import uk.ac.wellcome.models.work.test.util.WorksUtil

class DisplayPublicationDateV1SerialisationTest
    extends FunSpec
    with DisplaySerialisationTestBase
    with JsonMapperTestUtil
    with WorksUtil {

  it("omits the publicationDate field if it is empty") {
    val work = IdentifiedWork(
      canonicalId = "arfj5cj4",
      sourceIdentifier = sourceIdentifier,
      title = Some("Asking aging armadillos for another appraisal"),
      publicationDate = None,
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
                            |  "publicationDate": ${period(
                            work.publicationDate.get)},
                            |  "placesOfPublication": [ ]
                            |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

}
