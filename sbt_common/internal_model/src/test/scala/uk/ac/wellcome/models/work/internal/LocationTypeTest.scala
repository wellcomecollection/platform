package uk.ac.wellcome.models.work.internal

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException

class LocationTypeTest extends FunSpec with Matchers {
  it("looks up a location type") {
    LocationType("arch") shouldBe LocationType(
      id = "arch",
      label = "Archives Collection"
    )
  }

  it("throws an error if looking up a non-existent location type") {
    val caught = intercept[GracefulFailureException] {
      LocationType(id = "DoesNotExist")
    }
    caught.e.getMessage shouldBe "Unrecognised location type: [DoesNotExist]"
  }
}
