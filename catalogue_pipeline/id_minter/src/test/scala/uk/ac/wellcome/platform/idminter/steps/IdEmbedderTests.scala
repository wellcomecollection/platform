package uk.ac.wellcome.platform.idminter.steps

import io.circe.parser._
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.models.work.generators.WorksGenerators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

class IdEmbedderTests
    extends FunSpec
    with ScalaFutures
    with Matchers
    with MockitoSugar
    with Akka
    with JsonAssertions
    with IntegrationPatience
    with WorksGenerators {

  private def withIdEmbedder(
    testWith: TestWith[(IdentifierGenerator, IdEmbedder), Assertion]) = {
    withActorSystem { actorSystem =>
      val identifierGenerator: IdentifierGenerator =
        mock[IdentifierGenerator]

      val idEmbedder = new IdEmbedder(
        identifierGenerator = identifierGenerator
      )

      testWith((identifierGenerator, idEmbedder))
    }
  }

  it("sets the canonicalId given by the IdentifierGenerator on the work") {
    val originalWork = createUnidentifiedWork
    val newCanonicalId = createCanonicalId

    withIdEmbedder {
      case (identifierGenerator, idEmbedder) =>
        setUpIdentifierGeneratorMock(
          mockIdentifierGenerator = identifierGenerator,
          sourceIdentifier = originalWork.sourceIdentifier,
          ontologyType = originalWork.ontologyType,
          newCanonicalId = newCanonicalId
        )

        val newWorkFuture = idEmbedder.embedId(
          json = parse(
            toJson(originalWork).get
          ).right.get
        )

        val expectedWork = createIdentifiedWorkWith(
          canonicalId = newCanonicalId,
          title = originalWork.title,
          sourceIdentifier = originalWork.sourceIdentifier,
          version = originalWork.version
        )

        whenReady(newWorkFuture) { newWorkJson =>
          assertJsonStringsAreEqual(
            newWorkJson.toString(),
            toJson[IdentifiedBaseWork](expectedWork).get
          )
        }
    }
  }

  it("mints identifiers for creators in work") {
    val creatorIdentifier = createSourceIdentifierWith(
      ontologyType = "Person"
    )

    val person = Person(label = "The Librarian")
    val originalWork = createUnidentifiedWorkWith(
      contributors = List(
        Contributor(
          agent = Identifiable(person, sourceIdentifier = creatorIdentifier)
        )
      )
    )

    val newWorkCanonicalId = "5467"
    val newCreatorCanonicalId = "8901"

    withIdEmbedder {
      case (identifierGenerator, idEmbedder) =>
        setUpIdentifierGeneratorMock(
          mockIdentifierGenerator = identifierGenerator,
          sourceIdentifier = originalWork.sourceIdentifier,
          ontologyType = originalWork.ontologyType,
          newCanonicalId = newWorkCanonicalId
        )

        setUpIdentifierGeneratorMock(
          mockIdentifierGenerator = identifierGenerator,
          sourceIdentifier = creatorIdentifier,
          ontologyType = creatorIdentifier.ontologyType,
          newCanonicalId = newCreatorCanonicalId
        )

        val newWorkFuture = idEmbedder.embedId(
          json = parse(
            toJson(originalWork).get
          ).right.get
        )

        val expectedWork = createIdentifiedWorkWith(
          canonicalId = newWorkCanonicalId,
          title = originalWork.title,
          sourceIdentifier = originalWork.sourceIdentifier,
          contributors = List(
            Contributor(
              agent = Identified(
                agent = person,
                canonicalId = newCreatorCanonicalId,
                sourceIdentifier = creatorIdentifier))
          ),
          version = originalWork.version
        )

        whenReady(newWorkFuture) { newWorkJson =>
          assertJsonStringsAreEqual(
            newWorkJson.toString(),
            toJson[IdentifiedBaseWork](expectedWork).get
          )
        }
    }
  }

  it("returns a failed future if the call to IdentifierGenerator fails") {
    val originalWork = createUnidentifiedWork

    val expectedException = new Exception("Aaaaah something happened!")

    withIdEmbedder {
      case (identifierGenerator, idEmbedder) =>
        when(
          identifierGenerator
            .retrieveOrGenerateCanonicalId(
              originalWork.sourceIdentifier
            )
        ).thenReturn(Try(throw expectedException))

        val newWorkFuture =
          idEmbedder.embedId(json = parse(toJson(originalWork).get).right.get)

        whenReady(newWorkFuture.failed) { exception =>
          exception shouldBe expectedException
        }
    }
  }

  it("adds canonicalIds to all identifiable items") {
    val identifier = createSourceIdentifierWith(
      ontologyType = "Item"
    )

    val originalItem1 = Identifiable(
      sourceIdentifier = identifier,
      agent = Item(locations = List())
    )

    val originalItem2 = Unidentifiable(
      agent = Item(locations = List())
    )

    val originalWork = createUnidentifiedWorkWith(
      sourceIdentifier = identifier,
      items = List(originalItem1, originalItem2)
    )

    val newItemCanonicalId1 = "item1-canonical-id"

    withIdEmbedder {
      case (identifierGenerator, idEmbedder) =>
        setUpIdentifierGeneratorMock(
          mockIdentifierGenerator = identifierGenerator,
          sourceIdentifier = originalWork.sourceIdentifier,
          ontologyType = originalWork.ontologyType,
          newCanonicalId = "work-canonical-id"
        )

        setUpIdentifierGeneratorMock(
          mockIdentifierGenerator = identifierGenerator,
          sourceIdentifier = originalItem1.sourceIdentifier,
          ontologyType = originalItem1.agent.ontologyType,
          newCanonicalId = newItemCanonicalId1
        )

        val eventualWork = idEmbedder.embedId(
          parse(
            toJson(originalWork).get
          ).right.get
        )

        val expectedItem1: Displayable[Item] = createIdentifiedItemWith(
          sourceIdentifier = originalItem1.sourceIdentifier,
          canonicalId = newItemCanonicalId1,
          locations = originalItem1.agent.locations
        )

        val expectedItem2: Displayable[Item] = createUnidentifiableItemWith(
          locations = originalItem2.agent.locations
        )

        whenReady(eventualWork) { json =>
          val work = fromJson[IdentifiedWork](json.toString()).get

          val actualItem1 = work.items.head
          val actualItem2 = work.items.tail.head

          assertJsonStringsAreEqual(
            toJson(actualItem1).get,
            toJson(expectedItem1).get
          )

          assertJsonStringsAreEqual(
            toJson(actualItem2).get,
            toJson(expectedItem2).get
          )
        }
    }
  }

  describe("unidentifiable objects should pass through unchanged") {
    it("an empty map") {
      assertIdEmbedderDoesNothing("""{}""")
    }

    it("a map with some string keys") {
      assertIdEmbedderDoesNothing("""{
        "so": "sofia",
        "sk": "skopje"
      }""")
    }

    it("a map with some list objects") {
      assertIdEmbedderDoesNothing("""{
        "te": "tehran",
        "ta": [
          "tallinn",
          "tashkent"
        ]
      }""")
    }

    it("a complex nested structure") {
      assertIdEmbedderDoesNothing("""{
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

  describe("identifiable objects should be updated correctly") {

    it("identify a document that is Identifiable") {
      val sourceIdentifier = createSourceIdentifierWith(
        ontologyType = "false capitals"
      )
      val newCanonicalId = createCanonicalId

      withIdEmbedder {
        case (identifierGenerator, idEmbedder) =>
          setUpIdentifierGeneratorMock(
            mockIdentifierGenerator = identifierGenerator,
            sourceIdentifier = sourceIdentifier,
            ontologyType = sourceIdentifier.ontologyType,
            newCanonicalId = newCanonicalId
          )

          val inputJson = s"""
        {
          "sourceIdentifier": {
            "identifierType": {
              "id": "${sourceIdentifier.identifierType.id}",
              "label": "${sourceIdentifier.identifierType.label}",
              "ontologyType": "${sourceIdentifier.identifierType.ontologyType}"
            },
            "ontologyType": "${sourceIdentifier.ontologyType}",
            "value": "${sourceIdentifier.value}"
          },
          "ontologyType": "${sourceIdentifier.ontologyType}"
        }
        """

          val outputJson = s"""
        {
          "canonicalId": "$newCanonicalId",
          "sourceIdentifier": {
            "identifierType": {
              "id": "${sourceIdentifier.identifierType.id}",
              "label": "${sourceIdentifier.identifierType.label}",
              "ontologyType": "${sourceIdentifier.identifierType.ontologyType}"
            },
            "ontologyType": "${sourceIdentifier.ontologyType}",
            "value": "${sourceIdentifier.value}"
          },
          "ontologyType": "${sourceIdentifier.ontologyType}"
        }
        """

          val eventualJson = idEmbedder.embedId(parse(inputJson).right.get)

          whenReady(eventualJson) { json =>
            assertJsonStringsAreEqual(json.toString, outputJson)
          }
      }
    }

    it("identify a document with a key that is identifiable") {
      val sourceIdentifier = createSourceIdentifierWith(
        ontologyType = "fictional cities"
      )

      val newCanonicalId = createCanonicalId

      withIdEmbedder {
        case (identifierGenerator, idEmbedder) =>
          setUpIdentifierGeneratorMock(
            mockIdentifierGenerator = identifierGenerator,
            sourceIdentifier = sourceIdentifier,
            ontologyType = sourceIdentifier.ontologyType,
            newCanonicalId = newCanonicalId
          )

          val inputJson = s"""
        {
          "ke": null,
          "ki": "kiev",
          "item": {
            "sourceIdentifier": {
              "identifierType": {
                "id": "${sourceIdentifier.identifierType.id}",
                "label": "${sourceIdentifier.identifierType.label}",
                "ontologyType": "${sourceIdentifier.identifierType.ontologyType}"
              },
              "ontologyType": "${sourceIdentifier.ontologyType}",
              "value": "${sourceIdentifier.value}"
            },
            "ontologyType": "${sourceIdentifier.ontologyType}"
          }
        }
        """

          val outputJson = s"""
        {
          "ke": null,
          "ki": "kiev",
          "item": {
            "canonicalId": "$newCanonicalId",
            "sourceIdentifier": {
              "identifierType": {
                "id": "${sourceIdentifier.identifierType.id}",
                "label": "${sourceIdentifier.identifierType.label}",
                "ontologyType": "${sourceIdentifier.identifierType.ontologyType}"
              },
              "ontologyType": "${sourceIdentifier.ontologyType}",
              "value": "${sourceIdentifier.value}"
            },
            "ontologyType": "${sourceIdentifier.ontologyType}"
          }
        }
        """

          val eventualJson = idEmbedder.embedId(parse(inputJson).right.get)

          whenReady(eventualJson) { json =>
            assertJsonStringsAreEqual(json.toString, outputJson)
          }
      }
    }
  }

  it("sets the new type if the field identifiedType is present") {
    val sourceIdentifier = createSourceIdentifierWith(
      ontologyType = "false capitals"
    )

    val newCanonicalId = createCanonicalId

    withIdEmbedder {
      case (identifierGenerator, idEmbedder) =>
        setUpIdentifierGeneratorMock(
          mockIdentifierGenerator = identifierGenerator,
          sourceIdentifier = sourceIdentifier,
          ontologyType = sourceIdentifier.ontologyType,
          newCanonicalId = newCanonicalId
        )

        val inputJson = s"""
        {
          "sourceIdentifier": {
            "identifierType": {
              "id": "${sourceIdentifier.identifierType.id}",
              "label": "${sourceIdentifier.identifierType.label}",
              "ontologyType": "${sourceIdentifier.identifierType.ontologyType}"
            },
            "ontologyType": "${sourceIdentifier.ontologyType}",
            "value": "${sourceIdentifier.value}"
          },
          "identifiedType": "NewType",
          "ontologyType": "${sourceIdentifier.ontologyType}"
        }
        """

        val outputJson = s"""
        {
          "canonicalId": "$newCanonicalId",
          "sourceIdentifier": {
            "identifierType": {
              "id": "${sourceIdentifier.identifierType.id}",
              "label": "${sourceIdentifier.identifierType.label}",
              "ontologyType": "${sourceIdentifier.identifierType.ontologyType}"
            },
            "ontologyType": "${sourceIdentifier.ontologyType}",
            "value": "${sourceIdentifier.value}"
          },
          "type": "NewType",
          "ontologyType": "${sourceIdentifier.ontologyType}"
        }
        """

        val eventualJson = idEmbedder.embedId(parse(inputJson).right.get)

        whenReady(eventualJson) { json =>
          assertJsonStringsAreEqual(json.toString, outputJson)
        }
    }
  }

  private def setUpIdentifierGeneratorMock(
    mockIdentifierGenerator: IdentifierGenerator,
    sourceIdentifier: SourceIdentifier,
    ontologyType: String,
    newCanonicalId: String) = {
    when(
      mockIdentifierGenerator
        .retrieveOrGenerateCanonicalId(
          sourceIdentifier
        )
    ).thenReturn(Try(newCanonicalId))
  }

  private def assertIdEmbedderDoesNothing(jsonString: String) = {
    withIdEmbedder {
      case (_, idEmbedder) =>
        val eventualJson = idEmbedder.embedId(parse(jsonString).right.get)
        whenReady(eventualJson) { json =>
          assertJsonStringsAreEqual(json.toString(), jsonString)
        }
    }
  }

}
