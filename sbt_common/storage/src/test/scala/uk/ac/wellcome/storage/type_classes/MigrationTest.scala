package uk.ac.wellcome.storage.type_classes

import org.scalatest.{FunSpec, Matchers}
import Migration._

class MigrationTest extends FunSpec with Matchers {

  case class Alphabet(a: String, b: String, c: String, d: String, e: String)

  val alphabet = Alphabet(
    a = "apple",
    b = "banana",
    c = "coconut",
    d = "dandelion",
    e = "egg")

  it("gets the intersection of two case classes") {
    case class Vowels(a: String, e: String)
    alphabet.migrateTo[Vowels] shouldBe Vowels(
      a = alphabet.a,
      e = alphabet.e
    )
  }

  it("finds case class fields in the wrong order") {
    case class Consonants(d: String, c: String, b: String)
    alphabet.migrateTo[Consonants] shouldBe Consonants(
      d = alphabet.d,
      c = alphabet.c,
      b = alphabet.b
    )
  }
}
