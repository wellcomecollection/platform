package uk.ac.wellcome.display.test.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.{Guice, Injector}
import io.circe.Encoder
import org.scalatest.Assertion
import uk.ac.wellcome.display.json.DisplayJsonUtil
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.json.utils.JsonAssertions

trait JsonMapperTestUtil extends JsonAssertions {

  private val injector: Injector = Guice.createInjector(DisplayJacksonModule)
  private val objectMapper: ObjectMapper =
    injector.getInstance(classOf[ObjectMapper])

  def assertObjectMapsToJson[T](value: T, expectedJson: String)(
    implicit encoder: Encoder[T]): Assertion = {

    // First test it with Circe...
    assertJsonStringsAreEqual(DisplayJsonUtil.toJson(value), expectedJson)

    // ...and then with Jackson
    assertJsonStringsAreEqual(
      objectMapper.writeValueAsString(value),
      expectedJson)
  }
}
