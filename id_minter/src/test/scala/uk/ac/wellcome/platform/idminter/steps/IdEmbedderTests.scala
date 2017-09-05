package uk.ac.wellcome.platform.idminter.steps

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{Item, SourceIdentifier, Work}

import scala.util.Try

class IdEmbedderTests
  extends FunSpec
    with ScalaFutures
    with Matchers
    with BeforeAndAfterEach
    with MockitoSugar {

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
    when(
      mockIdentifierGenerator
        .retrieveOrGenerateCanonicalId(identifiers, originalWork.ontologyType))
      .thenReturn(Try(newCanonicalId))

    val newWorkFuture = idEmbedder.embedId(json = originalWork.asJson)

    whenReady(newWorkFuture) {newWork =>
      newWork shouldBe originalWork.copy(canonicalId = Some(newCanonicalId))
    }
  }

  it("should return a filed future if the call to IdentifierGenerator fails") {
    val identifiers =
      List(SourceIdentifier(IdentifierSchemes.miroImageNumber, value = "1234"))
    val originalWork =
      Work(identifiers = identifiers, title = "crap", canonicalId = None)
    val expectedException = new Exception("Aaaaah something happened!")
    when(
      mockIdentifierGenerator
        .retrieveOrGenerateCanonicalId(identifiers, originalWork.ontologyType))
      .thenReturn(Try(throw expectedException))

    val newWorkFuture = idEmbedder.embedId(json = originalWork.asJson)

    whenReady(newWorkFuture.failed) {exception =>
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
    val newItemCanonicalId2 = "item1-canonical-id"
    when(
      mockIdentifierGenerator
        .retrieveOrGenerateCanonicalId(identifiers, originalWork.ontologyType))
      .thenReturn(Try("work-canonical-id"))
    when(
      mockIdentifierGenerator
        .retrieveOrGenerateCanonicalId(originalItem1.identifiers, originalItem1.ontologyType))
      .thenReturn(Try(newItemCanonicalId1))
    when(
      mockIdentifierGenerator
        .retrieveOrGenerateCanonicalId(originalItem2.identifiers, originalItem2.ontologyType))
      .thenReturn(Try(newItemCanonicalId2))

    val eventualWork = idEmbedder.embedId(originalWork.asJson)

    whenReady(eventualWork) { json =>
      val work = decode[Work](json.toString()).right.get
      work.items.head shouldBe originalItem1.copy(
        canonicalId = Some(newItemCanonicalId1))
      work.items.tail.head shouldBe originalItem2.copy(
        canonicalId = Some(newItemCanonicalId2))
    }

  }

}