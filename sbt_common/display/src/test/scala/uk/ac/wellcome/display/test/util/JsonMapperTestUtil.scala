package uk.ac.wellcome.display.test.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.{Guice, Injector}
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.json.utils.JsonAssertions

trait JsonMapperTestUtil extends JsonAssertions {

  val injector: Injector = Guice.createInjector(DisplayJacksonModule)
  val objectMapper: ObjectMapper = injector.getInstance(classOf[ObjectMapper])

  def assertObjectMapsToJson(displayObject: Any, expectedJson: String) = {
    assertJsonStringsAreEqual(
      objectMapper.writeValueAsString(displayObject),
      expectedJson)
  }
}
