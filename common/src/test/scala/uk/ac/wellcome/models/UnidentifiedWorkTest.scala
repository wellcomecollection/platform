package uk.ac.wellcome.models

import org.scalacheck.Shrink
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil._
import org.scalacheck.ScalacheckShapeless._

class UnidentifiedWorkTest
    extends FunSpec
    with Matchers
    with JsonTestUtil
    with PropertyChecks {

  implicit val noShrink = Shrink.shrinkAny[UnidentifiedWork]

  it(
    "json serialisation and deserialisation doesn't change the original UnidenfiedWork") {
    forAll { unidentifiedWork: UnidentifiedWork =>
      val json = toJson(unidentifiedWork).get

      val deserialisedWork = fromJson[UnidentifiedWork](json).get

      deserialisedWork shouldBe unidentifiedWork
    }
  }
}
