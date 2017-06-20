package uk.ac.wellcome.platform.api.models

import org.scalatest.{FunSpec, Matchers}

class WorksIncludesTest extends FunSpec with Matchers {

  it("should use default values if nothing is provided") {
    val includes = WorksIncludes(None)
    includes.identifiers shouldBe false
  }

  it("should default a missing value to false") {
    val includes = WorksIncludes("")
    includes.identifiers shouldBe false
  }

  it("should parse a present value as true") {
    val includes = WorksIncludes("identifiers")
    includes.identifiers shouldBe true
  }

  it("should successfully validate a correct string") {
    val result = WorksIncludes.validate(queryParam = "identifiers")
    result.isLeft shouldBe true
  }

  it("should successfully validate if no query parameter is provided") {
    val result = WorksIncludes.validate(queryParam = None)
    result.isLeft shouldBe true
  }

  it("should successfully reject an correct string") {
    val result = WorksIncludes.validate(queryParam = "foo,bar")
    result shouldBe Right(List("foo", "bar"))
  }
}
