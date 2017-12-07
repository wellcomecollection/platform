package uk.ac.wellcome.platform.idminter.steps

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import io.circe.parser._
import org.mockito.Mockito.when
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time._
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{IdentifierSchemes, Item, SourceIdentifier, Work}
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil

import scala.util.Try

class IdEmbedderTests
    extends FunSpec
    with ScalaFutures
    with Matchers
    with BeforeAndAfterEach
    with MockitoSugar
    with JsonTestUtil
    with PatienceConfiguration {
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(3, Seconds)),
    interval = scaled(Span(50, Millis))
  )

  private val metricsSender =
    new MetricsSender("id_minter_test_metrics", mock[AmazonCloudWatch])
  private val mockIdentifierGenerator: IdentifierGenerator =
    mock[IdentifierGenerator]

  val idEmbedder = new IdEmbedder(
    metricsSender,
    mockIdentifierGenerator
  )

  it("should set the canonicalId given by the IdentifierGenerator on the work") {
    val identifiers =
      List(SourceIdentifier(IdentifierSchemes.miroImageNumber, value = "1234"))
    val originalWork =
      Work(identifiers = identifiers, title = "crap", canonicalId = None)
    val newCanonicalId = "5467"
    setUpIdentifierGeneratorMock(identifiers,
                                 originalWork.ontologyType,
                                 newCanonicalId)

    val newWorkFuture = idEmbedder.embedId(
      json = parse(JsonUtil.toJson(originalWork).get).right.get)

    val expectedWork = originalWork.copy(canonicalId = Some(newCanonicalId))

    whenReady(newWorkFuture) { newWorkJson =>
      assertJsonStringsAreEqual(newWorkJson.toString(),
                                JsonUtil.toJson(expectedWork).get)
    }
  }

  it("should return a failed future if the call to IdentifierGenerator fails") {
    val identifiers =
      List(SourceIdentifier(IdentifierSchemes.miroImageNumber, value = "1234"))
    val originalWork =
      Work(identifiers = identifiers, title = "crap", canonicalId = None)
    val expectedException = new Exception("Aaaaah something happened!")
    when(
      mockIdentifierGenerator
        .retrieveOrGenerateCanonicalId(identifiers, originalWork.ontologyType))
      .thenReturn(Try(throw expectedException))

    val newWorkFuture = idEmbedder.embedId(
      json = parse(JsonUtil.toJson(originalWork).get).right.get)

    whenReady(newWorkFuture.failed) { exception =>
      exception shouldBe expectedException
    }
  }

  it("should add canonicalIds to all items") {
    val identifiers =
      List(SourceIdentifier(IdentifierSchemes.miroImageNumber, value = "1234"))
    val originalItem1 =
      Item(canonicalId = None, identifiers = identifiers, locations = List())
    val originalItem2 =
      Item(
        canonicalId = None,
        identifiers = List(
          SourceIdentifier(IdentifierSchemes.miroImageNumber, value = "1235")),
        locations = List())
    val originalWork =
      Work(identifiers = identifiers,
           title = "crap",
           canonicalId = None,
           items = List(originalItem1, originalItem2))
    val newItemCanonicalId1 = "item1-canonical-id"
    val newItemCanonicalId2 = "item2-canonical-id"

    setUpIdentifierGeneratorMock(identifiers,
                                 originalWork.ontologyType,
                                 "work-canonical-id")
    setUpIdentifierGeneratorMock(originalItem1.identifiers,
                                 originalItem1.ontologyType,
                                 newItemCanonicalId1)
    setUpIdentifierGeneratorMock(originalItem2.identifiers,
                                 originalItem2.ontologyType,
                                 newItemCanonicalId2)

    val eventualWork =
      idEmbedder.embedId(parse(JsonUtil.toJson(originalWork).get).right.get)

    val expectedItem1 =
      originalItem1.copy(canonicalId = Some(newItemCanonicalId1))
    val expectedItem2 =
      originalItem2.copy(canonicalId = Some(newItemCanonicalId2))

    whenReady(eventualWork) { json =>
      val work = JsonUtil.fromJson[Work](json.toString()).get

      val actualItem1 = work.items.head
      val actualItem2 = work.items.tail.head

      assertJsonStringsAreEqual(JsonUtil.toJson(actualItem1).get,
                                JsonUtil.toJson(expectedItem1).get)
      assertJsonStringsAreEqual(JsonUtil.toJson(actualItem2).get,
                                JsonUtil.toJson(expectedItem2).get)
    }

  }

  describe(
    "Documents with no Identifiable objects should pass through unchanged") {
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

  describe(
    "Documents with Identifiable structures should be updated correctly") {
    it("identify a document that is itself Identifiable") {
      val sourceIdentifiers =
        List(SourceIdentifier(IdentifierSchemes.miroImageNumber, "sydney"))
      val ontologyType = "false capitals"
      val newCanonicalId =
        generateMockCanonicalId(sourceIdentifiers, ontologyType)

      setUpIdentifierGeneratorMock(sourceIdentifiers,
                                   ontologyType,
                                   newCanonicalId)

      assertIdEmbedderAddsCanonicalIdCorrectly(s"""
      {
        "canonicalId": "$newCanonicalId",
        "identifiers": [{
          "identifierScheme": "${sourceIdentifiers.head.identifierScheme}",
          "value": "${sourceIdentifiers.head.value}"
        }],
        "type": "$ontologyType"
      }
      """)
    }

    it("identify a document with a key that is identifiable") {
      val sourceIdentifiers =
        List(
          SourceIdentifier(IdentifierSchemes.miroImageNumber,
                           "king's landing"))
      val ontologyType = "fictional cities"
      val newCanonicalId =
        generateMockCanonicalId(sourceIdentifiers, ontologyType)
      setUpIdentifierGeneratorMock(sourceIdentifiers,
                                   ontologyType,
                                   newCanonicalId)
      assertIdEmbedderAddsCanonicalIdCorrectly(s"""
      {
        "ke": null,
        "ki": "kiev",
        "item": {
          "canonicalId": "$newCanonicalId",
          "identifiers": [{
            "identifierScheme": "${sourceIdentifiers.head.identifierScheme}",
            "value": "${sourceIdentifiers.head.value}"
          }],
          "type": "$ontologyType"
        }
      }
      """)
    }

    it("should pick up all the source identifiers that are available") {
      val sourceIdentifiersA = List(
        SourceIdentifier(IdentifierSchemes.miroImageNumber, "husvik"),
        SourceIdentifier(IdentifierSchemes.miroImageNumber, "spinalonga"),
        SourceIdentifier(IdentifierSchemes.miroImageNumber, "waterfoot")
      )
      val ontologyTypeA = "ghost towns"

      val newCanonicalIdA =
        generateMockCanonicalId(sourceIdentifiersA, ontologyTypeA)
      setUpIdentifierGeneratorMock(sourceIdentifiersA,
                                   ontologyTypeA,
                                   newCanonicalIdA)

      val sourceIdentifiersB = List(
        SourceIdentifier(IdentifierSchemes.miroImageNumber, "atlantis"),
        SourceIdentifier(IdentifierSchemes.miroImageNumber, "camelot")
      )
      val ontologyTypeB = "mythological places"

      val newCanonicalIdB =
        generateMockCanonicalId(sourceIdentifiersB, ontologyTypeB)
      setUpIdentifierGeneratorMock(sourceIdentifiersB,
                                   ontologyTypeB,
                                   newCanonicalIdB)

      val sourceIdentifiersC = List(
        SourceIdentifier(IdentifierSchemes.miroImageNumber, "lundenwic"),
        SourceIdentifier(IdentifierSchemes.miroImageNumber, "dunedin")
      )
      val ontologyTypeC = "cities that were renamed"

      val newCanonicalIdC =
        generateMockCanonicalId(sourceIdentifiersC, ontologyTypeC)
      setUpIdentifierGeneratorMock(sourceIdentifiersC,
                                   ontologyTypeC,
                                   newCanonicalIdC)

      assertIdEmbedderAddsCanonicalIdCorrectly(s"""
      {
        "items": [
          {
            "canonicalId": "$newCanonicalIdA",
            "identifiers": [
              {
                "identifierScheme": "${sourceIdentifiersA.head.identifierScheme}",
                "value": "${sourceIdentifiersA.head.value}"
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
            "type": "$ontologyTypeA"
          },
          {
            "canonicalId": "$newCanonicalIdB",
            "identifiers": [
              {
                "identifierScheme": "${sourceIdentifiersB.head.identifierScheme}",
                "value": "${sourceIdentifiersB.head.value}"
              },
              {
                "identifierScheme": "${sourceIdentifiersB(1).identifierScheme}",
                "value": "${sourceIdentifiersB(1).value}"
              }
            ],
            "type": "$ontologyTypeB"
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
            "canonicalId": "$newCanonicalIdC",
            "identifiers": [
              {
                "identifierScheme": "${sourceIdentifiersC.head.identifierScheme}",
                "value": "${sourceIdentifiersC.head.value}"
              },
              {
                "identifierScheme": "${sourceIdentifiersC(1).identifierScheme}",
                "value": "${sourceIdentifiersC(1).value}"
              }
            ],
            "type": "$ontologyTypeC"
          }
        ]
      }
      """)
    }
  }

  def generateMockCanonicalId(sourceIdentifiers: List[SourceIdentifier],
                              ontologyType: String): String = {
    val sourceIdentifiersStrings = sourceIdentifiers.map { _.toString }
    List(ontologyType, sourceIdentifiersStrings.mkString(";"))
      .mkString("==")
  }

  private def setUpIdentifierGeneratorMock(
    sourceIdentifiers: List[SourceIdentifier],
    ontologyType: String,
    newCanonicalId: String) = {
    when(
      mockIdentifierGenerator.retrieveOrGenerateCanonicalId(sourceIdentifiers,
                                                            ontologyType))
      .thenReturn(Try(newCanonicalId))
  }

  // Strip the canonical ID from a JSON string, then run it through the idEmbedder
  // and check it's reinserted correctly.
  private def assertIdEmbedderAddsCanonicalIdCorrectly(jsonString: String) = {
    val unidentifiedString = jsonString.lines
      .filterNot { _.contains("canonicalId") }
      .mkString("\n")
    unidentifiedString shouldNot be(jsonString)
    val eventualJson = idEmbedder.embedId(parse(unidentifiedString).right.get)

    whenReady(eventualJson) { json =>
      assertJsonStringsAreEqual(json.toString, jsonString)
    }
  }

  private def assertIdEmbedderDoesNothing(jsonString: String) = {
    val eventualJson = idEmbedder.embedId(parse(jsonString).right.get)
    whenReady(eventualJson) { json =>
      assertJsonStringsAreEqual(json.toString(), jsonString)
    }
  }

}
