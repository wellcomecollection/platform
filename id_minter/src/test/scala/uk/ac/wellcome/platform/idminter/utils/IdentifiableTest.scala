package uk.ac.wellcome.platform.idminter.utils

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.prop.PropertyChecks

class IdentifiableTest extends FunSpec with PropertyChecks with Matchers {

  it("should generate a string with 8 letters or digits") {
    forAll(minSuccessful(100)) { (a: Int) =>
      {
        val id = Identifiable.generate
        id should have size (8)
        id.toCharArray should contain noneOf ('o', 'i', 'l', '1')
      }
    }
  }

}
