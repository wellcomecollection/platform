package uk.ac.wellcome.models

import org.scalatest.prop.PropertyChecks
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.utils.JsonTestUtil
import org.scalacheck.ScalacheckShapeless._
import org.scalacheck.Shrink

class IdentifiedWorkTest
    extends FunSpec
    with Matchers
    with JsonTestUtil
    with PropertyChecks {

  implicit val noShrink = Shrink.shrinkAny[IdentifiedWork]

  it(
    "json serialisation and deserialisation doesn't change the original IdenfiedWork") {
    forAll { identifiedWork: IdentifiedWork =>
      val json = toJson(identifiedWork).get

      val deserialisedWork = fromJson[IdentifiedWork](json).get

      deserialisedWork shouldBe identifiedWork
    }
  }
}
