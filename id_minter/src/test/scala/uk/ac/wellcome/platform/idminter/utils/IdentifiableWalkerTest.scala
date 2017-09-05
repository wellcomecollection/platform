package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}

import com.fasterxml.jackson.databind.ObjectMapper

import uk.ac.wellcome.platform.idminter.utils.IdentifiableWalker


class IdentifiableWalkerTest
    extends FunSpec
    with Matchers {

  // Because the IdentifiableWalker works by walking the entire tree and
  // rebuilding a copy of it, we try some JSON structures that don't contain
  // anything Identifiable and check it isn't losing information.
  describe("Documents with no Identifiable objects should pass through unchanged") {
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

  val walker = IdentifiableWalker(generateCanonicalId = generateCanonicalId)

  // An in-memory canonical ID generator for use in testing.  This allows us
  // to write lots of fast tests for the tree-walking logic, and we can have
  // fewer, slower tests that make database calls.
  def generateCanonicalId(sourceIdentifiers: List[SourceIdentifier],
                          ontologyType: String): String = {
    val sourceIdentifiersStrings = sourceIdentifiers.map { _.toString }
    List(ontologyType, sourceIdentifiersStrings.mkString(";"))
      .mkString("==")
  }

  describe("Documents with Identifiable structures should be updated correctly") {
    it("identify a document that is itself Identifiable") {
      val sourceIdentifiers = List(SourceIdentifier("australia", "sydney"))
      val ontologyType = "false capitals"

      assertWalkerAddsCanonicalIdCorrectly(s"""
      {
        "canonicalId": "${generateCanonicalId(sourceIdentifiers, ontologyType)}",
        "identifiers": [{
          "identifierScheme": "${sourceIdentifiers(0).identifierScheme}",
          "value": "${sourceIdentifiers(0).value}"
        }],
        "type": "${ontologyType}"
      }
      """)
    }

    it("identify a document with a key that is identifiable") {
      val sourceIdentifiers = List(SourceIdentifier("westeros", "king's landing"))
      val ontologyType = "fictional cities"

      assertWalkerAddsCanonicalIdCorrectly(s"""
      {
        "ke": null,
        "ki": "kiev",
        "item": {
          "canonicalId": "${generateCanonicalId(sourceIdentifiers, ontologyType)}",
          "identifiers": [{
            "identifierScheme": "${sourceIdentifiers(0).identifierScheme}",
            "value": "${sourceIdentifiers(0).value}"
          }],
          "type": "${ontologyType}"
        }
      }
      """)
    }

    it("should pick up all the source identifiers that are available") {
      val sourceIdentifiersA = List(
        SourceIdentifier("antarctica", "husvik"),
        SourceIdentifier("greece", "spinalonga"),
        SourceIdentifier("ireland", "waterfoot")
      )
      val ontologyTypeA = "ghost towns"

      val sourceIdentifiersB = List(
        SourceIdentifier("ocean", "atlantis"),
        SourceIdentifier("regal", "camelot")
      )
      val ontologyTypeB = "mythological places"

      val sourceIdentifiersC = List(
        SourceIdentifier("england", "lundenwic"),
        SourceIdentifier("scotland", "dunedin")
      )
      val ontologyTypeC = "cities that were renamed"

      assertWalkerAddsCanonicalIdCorrectly(s"""
      {
        "items": [
          {
            "canonicalId": "${generateCanonicalId(sourceIdentifiersA, ontologyTypeA)}",
            "identifiers": [
              {
                "identifierScheme": "${sourceIdentifiersA(0).identifierScheme}",
                "value": "${sourceIdentifiersA(0).value}"
              },
              {
                "identifierScheme": "${sourceIdentifiersA(1).identifierScheme}",
                "value": "${sourceIdentifiersA(1).value}"
              },
              {
                "identifierScheme": "${sourceIdentifiersA(2).identifierScheme}",
                "value": "${sourceIdentifiersA(2).value}"
              }
            ],
            "type": "${ontologyTypeA}"
          },
          {
            "canonicalId": "${generateCanonicalId(sourceIdentifiersB, ontologyTypeB)}",
            "identifiers": [
              {
                "identifierScheme": "${sourceIdentifiersB(0).identifierScheme}",
                "value": "${sourceIdentifiersB(0).value}"
              },
              {
                "identifierScheme": "${sourceIdentifiersB(1).identifierScheme}",
                "value": "${sourceIdentifiersB(1).value}"
              }
            ],
            "type": "${ontologyTypeB}"
          }
        ],
        "title": "A letter about loss",
        "lettering": "things we lose have a way of coming back to us in the end, if not always in the way we expect",
        "subjects": [
          {
            "label": "Loss and grief",
            "type": "Concept"
          },
          {
            "label": "Resurrection and renaming",
            "canonicalId": "${generateCanonicalId(sourceIdentifiersC, ontologyTypeC)}",
            "identifiers": [
              {
                "identifierScheme": "${sourceIdentifiersC(0).identifierScheme}",
                "value": "${sourceIdentifiersC(0).value}"
              },
              {
                "identifierScheme": "${sourceIdentifiersC(1).identifierScheme}",
                "value": "${sourceIdentifiersC(1).value}"
              }
            ],
            "type": "${ontologyTypeC}"
          }
        ]
      }
      """)
    }
  }

  // Strip the canonical ID from a JSON string, then run it through the walker
  // and check it's reinserted correctly.
  private def assertWalkerAddsCanonicalIdCorrectly(jsonString: String) = {
    val unidentifiedString = jsonString
      .lines
      .filterNot { _.contains("canonicalId") }
      .mkString("\n")
    unidentifiedString shouldNot be(jsonString)
    assertJsonStringsAreEqual(
      walker.identifyDocument(unidentifiedString),
      jsonString)
  }

  private def assertWalkerDoesNothing(jsonString: String) =
    assertJsonStringsAreEqual(jsonString, walker.identifyDocument(jsonString))

  private def assertJsonStringsAreEqual(jsonString1: String, jsonString2: String) = {
    val mapper = new ObjectMapper()
    val node1 = mapper.readTree(jsonString1)
    val node2 = mapper.readTree(jsonString2)
    node1 shouldBe node2
  }
}
