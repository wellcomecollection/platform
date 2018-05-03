package uk.ac.wellcome.platform.idminter.utils

import org.scalatest.{FunSpec, Matchers}

class IdentifiableTest extends FunSpec with Matchers {

  it("generates an 8-char string, with no ambiguous characters") {
    (1 to 100).map { _ =>
      val id = Identifiable.generate
      id should have size (8)
      id.toCharArray should contain noneOf ('0', 'o', 'i', 'l', '1')
      id should fullyMatch regex "[0-9|a-z&&[^oil10]]{8}"
    }
  }

  it("should never generate an identifier that starts with a number") {
    (1 to 100).map { _ =>
      Identifiable.generate should not(startWith regex "[0-9]")
    }
  }
}
