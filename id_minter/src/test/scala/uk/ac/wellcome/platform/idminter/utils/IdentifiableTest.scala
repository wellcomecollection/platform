package uk.ac.wellcome.platform.idminter.utils

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.prop.PropertyChecks

class IdentifiableTest extends FunSpec with PropertyChecks with Matchers {

  it("should generate a string with 8 characters, none of which are ambiguous characters") {
    forAll(minSuccessful(100)) { (a: Int) =>
      {
        val id = Identifiable.generate
        id should have size (8)
        id.toCharArray should contain noneOf ('0', 'o', 'i', 'l', '1')
        id should fullyMatch regex "[0-9|a-z&&[^oil10]]{8}"
      }
    }
  }

  // We have a rule that identifiers can't start with a number, as this
  // makes them awkward to use as a string in certain contexts.
  it("should never generate an identifier that starts with a number") {
    forAll(minSuccessful(100)) { (_: Int) =>
      {
        val id = Identifiable.generate
        assert(!id.head.isDigit)
      }
    }
  }

}
