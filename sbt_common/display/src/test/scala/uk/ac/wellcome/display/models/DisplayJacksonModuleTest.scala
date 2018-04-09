package uk.ac.wellcome.display.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Guice
import org.scalatest.FunSpec
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.test.utils.JsonTestUtil

class DisplayJacksonModuleTest extends FunSpec with DisplayJacksonModuleTestBase with JsonTestUtil with WorksUtil {
  val injector = Guice.createInjector(DisplayJacksonModule)

  val objectMapper = injector.getInstance(classOf[ObjectMapper])

  it("should serialise a DisplayWork correctly") {

    val work = workWith(
      canonicalId = canonicalId,
      title = title,
      description = description,
      lettering = lettering,
      createdDate = period,
      creator = agent,
      items = List(defaultItem),
      visible = true)

    val actualJsonString = objectMapper.writeValueAsString(DisplayWork(work))

    val expectedJsonString = s"""
       |{
       | "type": "Work",
       | "id": "$canonicalId",
       | "title": "$title",
       | "description": "$description",
       | "workType": {
       |       "id": "${workType.id}",
       |       "label": "${workType.label}",
       |       "type": "WorkType"
       | },
       | "lettering": "$lettering",
       | "createdDate": ${period(work.createdDate.get)},
       | "creators": [ ${identifiedOrUnidentifiable(
      work.creators(0),
      abstractAgent)} ],
       | "subjects": [ ],
       | "genres": [ ],
       | "publishers": [ ],
       | "placesOfPublication": [ ]
       |}
          """.stripMargin

    assertJsonStringsAreEqual(actualJsonString, expectedJsonString)
  }
}
