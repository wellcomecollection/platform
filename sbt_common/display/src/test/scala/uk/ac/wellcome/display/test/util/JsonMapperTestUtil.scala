package uk.ac.wellcome.display.test.util

import io.circe.Encoder
import org.scalatest.Assertion
import uk.ac.wellcome.display.json.DisplayJsonUtil
import uk.ac.wellcome.json.utils.JsonAssertions

trait JsonMapperTestUtil extends JsonAssertions {
  def assertObjectMapsToJson[T](
    value: T,
    expectedJson: String)(implicit encoder: Encoder[T]): Assertion =
    assertJsonStringsAreEqual(
      DisplayJsonUtil.toJson(value),
      expectedJson)
}
