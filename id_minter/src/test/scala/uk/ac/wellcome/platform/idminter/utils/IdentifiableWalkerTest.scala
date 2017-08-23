package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}

import com.fasterxml.jackson.databind.ObjectMapper

import uk.ac.wellcome.platform.idminter.utils.IdentifiableWalker


class IdentifiableWalkerTest
    extends FunSpec
    with Matchers {

  /* Because the IdentifiableWalker works by walking the entire tree and
   * rebuilding a copy of it, we try some JSON structures that don't contain
   * anything Identifiable and check it isn't losing information.
   */
  describe("JSON strings with no Identifiable objects should pass through unchanged") {
    it("an empty map") {
      assertWalkerDoesNothing("""{}""")
    }

    it("a map with some string keys") {
      assertWalkerDoesNothing("""{
        "so": "sofia",
        "sk": "skopje"
      }""")
    }

    it("a map with some list objects") {
      assertWalkerDoesNothing("""{
        "te": "tehran",
        "ta": [
          "tallinn",
          "tashkent"
        ]
      }""")
    }

    it("a complex nested structure") {
      assertWalkerDoesNothing("""{
        "u": "ulan bator",
        "v": [
          "vatican city",
          {
            "vic": "victoria",
            "vie": "vienna",
            "vil": "vilnius"
          }
        ],
        "w": {
          "wa": [
            "warsaw",
            "washington dc"
          ],
          "we": "wellington",
          "wi": {
            "win": "windhoek"
          }
        }
      }""")
    }
  }

  private def assertWalkerDoesNothing(jsonString: String) = {
    assertJsonStringsAreEqual(jsonString, IdentifiableWalker.identifyDocument(jsonString))
  }

  private def assertJsonStringsAreEqual(jsonString1: String, jsonString2: String) = {
    val mapper = new ObjectMapper()
    val node1 = mapper.readTree(jsonString1)
    val node2 = mapper.readTree(jsonString2)
    node1 shouldBe node2
  }
}
