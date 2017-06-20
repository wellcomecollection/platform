package uk.ac.wellcome.platform.api.models

import org.scalatest.{FunSpec, Matchers}

class WorksIncludesTest extends FunSpec with Matchers {

  it("should default a missing value to false") {
    val includes = WorksIncludes("")
    includes.identifiers shouldBe false
  }

  it("should parse a present value as true") {
    val includes = WorksIncludes("identifiers")
    includes.identifiers shouldBe true
  }

  it("should successfully create from a correct string") {
    val result = WorksIncludes.create(queryParam = "identifiers")
    result.isRight shouldBe true
  }

  it("should successfully create if no query parameter is provided") {
    val result = WorksIncludes.create(queryParam = None)
    result.isRight shouldBe true
  }

  it("should successfully reject an correct string") {
    val result = WorksIncludes.create(queryParam = "foo,bar")
    result shouldBe Left(List("foo", "bar"))
  }
}
