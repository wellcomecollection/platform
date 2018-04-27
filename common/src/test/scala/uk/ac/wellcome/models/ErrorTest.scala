package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}

class ErrorTest extends FunSpec with Matchers {
  it("should create an HTTP 404 error response") {
    val description = "Work not found for identifier 1234"
    val error: Error =
      Error(variant = "http-404", description = Some(description))

    error.errorType shouldBe "http"
    error.httpStatus.get shouldBe 404
    error.label shouldBe "Not Found"
    error.description.get shouldBe description
  }
}