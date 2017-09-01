package uk.ac.wellcome.platform.idminter.steps

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.mockito.Mockito.when
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{SourceIdentifier, Work}

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
  val something = new IdEmbedder(
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

    val newWorkFuture = something.embedId(work = originalWork)

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

    val newWorkFuture = something.embedId(work = originalWork)

    whenReady(newWorkFuture.failed) {exception =>
      exception shouldBe expectedException
    }
  }

}