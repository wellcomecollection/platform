package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}

class WorksIncludesTest extends FunSpec with Matchers {

  it("should parse a present value as true") {
    val includes = WorksIncludes("identifiers")
    includes.identifiers shouldBe true
  }

  it("should successfully create if no query parameter is provided") {
    val includes = WorksIncludes(queryParam = None)
    includes.identifiers shouldBe false
  }

  it("should successfully reject an correct string") {
    intercept[WorksIncludesParsingException] {
      WorksIncludes(queryParam = "foo,bar")
    }
  }
}
