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
}
