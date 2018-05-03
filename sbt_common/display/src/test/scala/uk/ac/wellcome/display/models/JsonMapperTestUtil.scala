package uk.ac.wellcome.display.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.{Guice, Injector}
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.test.utils.JsonTestUtil

trait JsonMapperTestUtil extends JsonTestUtil {

  val injector: Injector = Guice.createInjector(DisplayJacksonModule)
  val objectMapper: ObjectMapper = injector.getInstance(classOf[ObjectMapper])

  def assertObjectMapsToJson(displayObject: Any, expectedJson: String) = {
    assertJsonStringsAreEqual(objectMapper.writeValueAsString(displayObject), expectedJson)
  }
}

