package uk.ac.wellcome.platform.idminter.steps

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import io.circe.parser._
import org.mockito.Mockito.when
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time._
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{IdentifierSchemes, Item, SourceIdentifier, Work}
import uk.ac.wellcome.test.utils.JsonTestUtil

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
    new MetricsSender("id_minter_test_metrics",
                      mock[AmazonCloudWatch],
                      ActorSystem())
  private val mockIdentifierGenerator: IdentifierGenerator =
    mock[IdentifierGenerator]

  val idEmbedder = new IdEmbedder(
    metricsSender,
    mockIdentifierGenerator
  )

  it("should set the canonicalId given by the IdentifierGenerator on the work") {
    val identifier = SourceIdentifier(
      IdentifierSchemes.miroImageNumber,
      value = "1234"
    )

    val originalWork = Work(title = Some("crap"),
                            sourceIdentifier = identifier,
      version = 1,
                            canonicalId = None)

    val newCanonicalId = "5467"

    setUpIdentifierGeneratorMock(
      identifier,
      originalWork.ontologyType,
      newCanonicalId
    )

    val newWorkFuture = idEmbedder.embedId(
      json = parse(
        toJson(originalWork).get
      ).right.get
    )

    val expectedWork = originalWork.copy(canonicalId = Some(newCanonicalId))

    whenReady(newWorkFuture) { newWorkJson =>
      assertJsonStringsAreEqual(
        newWorkJson.toString(),
        toJson(expectedWork).get
      )
    }
  }

  it("should return a failed future if the call to IdentifierGenerator fails") {
    val identifier = SourceIdentifier(
      IdentifierSchemes.miroImageNumber,
      value = "1234"
    )

    val originalWork = Work(title = Some("crap"),
                            sourceIdentifier = identifier,
      version = 1,
                            canonicalId = None)

    val expectedException = new Exception("Aaaaah something happened!")

    when(
      mockIdentifierGenerator
        .retrieveOrGenerateCanonicalId(
          identifier,
          originalWork.ontologyType
        )
    ).thenReturn(Try(throw expectedException))

    val newWorkFuture =
      idEmbedder.embedId(json = parse(toJson(originalWork).get).right.get)

    whenReady(newWorkFuture.failed) { exception =>
      exception shouldBe expectedException
    }
  }

  it("should add canonicalIds to all items") {
    val identifier = SourceIdentifier(
      IdentifierSchemes.miroImageNumber,
      value = "1234"
    )

    val originalItem1 = Item(
      canonicalId = None,
      sourceIdentifier = identifier,
      locations = List()
    )

    val originalItem2 = Item(
      canonicalId = None,
      sourceIdentifier = SourceIdentifier(
        IdentifierSchemes.miroImageNumber,
        value = "1235"
      ),
      locations = List()
    )

    val originalWork = Work(title = Some("crap"),
                            sourceIdentifier = identifier,
      version =1,
                            canonicalId = None,
                            items = List(originalItem1, originalItem2))

    val newItemCanonicalId1 = "item1-canonical-id"
    val newItemCanonicalId2 = "item2-canonical-id"

    setUpIdentifierGeneratorMock(
      identifier,
      originalWork.ontologyType,
      "work-canonical-id"
    )

    setUpIdentifierGeneratorMock(
      originalItem1.sourceIdentifier,
      originalItem1.ontologyType,
      newItemCanonicalId1
    )

    setUpIdentifierGeneratorMock(
      originalItem2.sourceIdentifier,
      originalItem2.ontologyType,
      newItemCanonicalId2
    )

    val eventualWork = idEmbedder.embedId(
      parse(
        toJson(originalWork).get
      ).right.get
    )

    val expectedItem1 = originalItem1.copy(
      canonicalId = Some(newItemCanonicalId1)
    )

    val expectedItem2 = originalItem2.copy(
      canonicalId = Some(newItemCanonicalId2)
    )

    whenReady(eventualWork) { json =>
      val work = fromJson[Work](json.toString()).get

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

      val sourceIdentifier = SourceIdentifier(
        IdentifierSchemes.miroImageNumber,
        "sydney"
      )

      val ontologyType = "false capitals"
      val newCanonicalId =
        generateMockCanonicalId(sourceIdentifier, ontologyType)

      setUpIdentifierGeneratorMock(
        sourceIdentifier,
        ontologyType,
        newCanonicalId
      )

      val inputJson = s"""
      {
        "sourceIdentifier": {
          "identifierScheme": "${sourceIdentifier.identifierScheme}",
          "value": "${sourceIdentifier.value}"
        },
        "type": "$ontologyType"
      }
      """

      val outputJson = s"""
      {
        "canonicalId": "$newCanonicalId",
        "sourceIdentifier": {
          "identifierScheme": "${sourceIdentifier.identifierScheme}",
          "value": "${sourceIdentifier.value}"
        },
        "type": "$ontologyType"
      }
      """

      val eventualJson = idEmbedder.embedId(parse(inputJson).right.get)

      whenReady(eventualJson) { json =>
        assertJsonStringsAreEqual(json.toString, outputJson)
      }
    }

    it("identify a document with a key that is identifiable") {
      val sourceIdentifier = SourceIdentifier(
        IdentifierSchemes.miroImageNumber,
        "king's landing"
      )

      val ontologyType = "fictional cities"

      val newCanonicalId = generateMockCanonicalId(
        sourceIdentifier,
        ontologyType
      )

      setUpIdentifierGeneratorMock(
        sourceIdentifier,
        ontologyType,
        newCanonicalId
      )

      val inputJson = s"""
      {
        "ke": null,
        "ki": "kiev",
        "item": {
          "sourceIdentifier": {
            "identifierScheme": "${sourceIdentifier.identifierScheme}",
            "value": "${sourceIdentifier.value}"
          },
          "type": "$ontologyType"
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
            "identifierScheme": "${sourceIdentifier.identifierScheme}",
            "value": "${sourceIdentifier.value}"
          },
          "type": "$ontologyType"
        }
      }
      """

      val eventualJson = idEmbedder.embedId(parse(inputJson).right.get)

      whenReady(eventualJson) { json =>
        assertJsonStringsAreEqual(json.toString, outputJson)
      }
    }
  }

  def generateMockCanonicalId(
    sourceIdentifier: SourceIdentifier,
    ontologyType: String
  ): String =
    s"${sourceIdentifier.identifierScheme.toString}==${sourceIdentifier.value}"

  private def setUpIdentifierGeneratorMock(sourceIdentifier: SourceIdentifier,
                                           ontologyType: String,
                                           newCanonicalId: String) = {
    when(
      mockIdentifierGenerator
        .retrieveOrGenerateCanonicalId(
          sourceIdentifier,
          ontologyType
        )
    ).thenReturn(Try(newCanonicalId))
  }

  private def assertIdEmbedderDoesNothing(jsonString: String) = {
    val eventualJson = idEmbedder.embedId(parse(jsonString).right.get)
    whenReady(eventualJson) { json =>
      assertJsonStringsAreEqual(json.toString(), jsonString)
    }
  }

}
