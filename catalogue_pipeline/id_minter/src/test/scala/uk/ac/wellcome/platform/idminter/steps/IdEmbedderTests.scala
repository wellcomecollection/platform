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
import uk.ac.wellcome.models._
import uk.ac.wellcome.test.utils.JsonTestUtil
import scala.concurrent.duration._

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
    new MetricsSender(
      "id_minter_test_metrics",
      100 milliseconds,
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
      identifierScheme = IdentifierSchemes.miroImageNumber,
      ontologyType = "Work",
      value = "1234"
    )

    val originalWork = UnidentifiedWork(
      title = Some("crap"),
      sourceIdentifier = identifier,
      version = 1)

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

    val expectedWork = IdentifiedWork(
      canonicalId = newCanonicalId,
      title = originalWork.title,
      sourceIdentifier = originalWork.sourceIdentifier,
      version = originalWork.version
    )

    whenReady(newWorkFuture) { newWorkJson =>
      assertJsonStringsAreEqual(
        newWorkJson.toString(),
        toJson(expectedWork).get
      )
    }
  }

  it("mints identifiers for creators in work") {
    val workIdentifier = SourceIdentifier(
      IdentifierSchemes.miroImageNumber,
      ontologyType = "Work",
      value = "1234"
    )

    val creatorIdentifier = SourceIdentifier(
      IdentifierSchemes.libraryOfCongressNames,
      ontologyType = "Person",
      value = "1234"
    )

    val person = Person(label = "The Librarian")
    val originalWork = UnidentifiedWork(
      title = Some("crap"),
      sourceIdentifier = workIdentifier,
      contributors = List(
        Contributor(agent = Identifiable(
          person,
          sourceIdentifier = creatorIdentifier,
          identifiers = List(creatorIdentifier)))
      ),
      version = 1
    )

    val newWorkCanonicalId = "5467"

    setUpIdentifierGeneratorMock(
      workIdentifier,
      originalWork.ontologyType,
      newWorkCanonicalId
    )
    val newCreatorCanonicalId = "8901"

    setUpIdentifierGeneratorMock(
      creatorIdentifier,
      "Person",
      newCreatorCanonicalId
    )

    val newWorkFuture = idEmbedder.embedId(
      json = parse(
        toJson(originalWork).get
      ).right.get
    )

    val expectedWork = IdentifiedWork(
      canonicalId = newWorkCanonicalId,
      title = originalWork.title,
      sourceIdentifier = originalWork.sourceIdentifier,
      contributors = List(
        Contributor(agent =  Identified(
          agent = person,
          canonicalId = newCreatorCanonicalId,
          identifiers = List(creatorIdentifier)))
      ),
      version = originalWork.version
    )

    whenReady(newWorkFuture) { newWorkJson =>
      assertJsonStringsAreEqual(
        newWorkJson.toString(),
        toJson(expectedWork).get
      )
    }
  }

  it("should return a failed future if the call to IdentifierGenerator fails") {
    val identifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.miroImageNumber,
      ontologyType = "Work",
      value = "1234"
    )

    val originalWork = UnidentifiedWork(
      title = Some("crap"),
      sourceIdentifier = identifier,
      version = 1)

    val expectedException = new Exception("Aaaaah something happened!")

    when(
      mockIdentifierGenerator
        .retrieveOrGenerateCanonicalId(
          identifier
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
      ontologyType = "Item",
      value = "1234"
    )

    val originalItem1 = UnidentifiedItem(
      sourceIdentifier = identifier,
      locations = List()
    )

    val originalItem2 = UnidentifiedItem(
      sourceIdentifier = SourceIdentifier(
        IdentifierSchemes.miroImageNumber,
        ontologyType = "Item",
        value = "1235"
      ),
      locations = List()
    )

    val originalWork = UnidentifiedWork(
      title = Some("crap"),
      sourceIdentifier = identifier,
      version = 1,
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

    val expectedItem1 = IdentifiedItem(
      sourceIdentifier = originalItem1.sourceIdentifier,
      canonicalId = newItemCanonicalId1
    )

    val expectedItem2 = IdentifiedItem(
      sourceIdentifier = originalItem2.sourceIdentifier,
      canonicalId = newItemCanonicalId2
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

      val ontologyType = "false capitals"
      val sourceIdentifier = SourceIdentifier(
        IdentifierSchemes.miroImageNumber,
        ontologyType = ontologyType,
        "sydney"
      )

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
          "ontologyType": "$ontologyType",
          "value": "${sourceIdentifier.value}"
        },
        "ontologyType": "$ontologyType"
      }
      """

      val outputJson = s"""
      {
        "canonicalId": "$newCanonicalId",
        "sourceIdentifier": {
          "identifierScheme": "${sourceIdentifier.identifierScheme}",
          "ontologyType": "$ontologyType",
          "value": "${sourceIdentifier.value}"
        },
        "ontologyType": "$ontologyType"
      }
      """

      val eventualJson = idEmbedder.embedId(parse(inputJson).right.get)

      whenReady(eventualJson) { json =>
        assertJsonStringsAreEqual(json.toString, outputJson)
      }
    }

    it("identify a document with a key that is identifiable") {
      val ontologyType = "fictional cities"

      val sourceIdentifier = SourceIdentifier(
        IdentifierSchemes.miroImageNumber,
        ontologyType = ontologyType,
        "king's landing"
      )

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
            "ontologyType": "$ontologyType",
            "value": "${sourceIdentifier.value}"
          },
          "ontologyType": "$ontologyType"
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
            "ontologyType": "$ontologyType",
            "value": "${sourceIdentifier.value}"
          },
          "ontologyType": "$ontologyType"
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
          sourceIdentifier
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
