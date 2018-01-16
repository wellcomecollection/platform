package uk.ac.wellcome.platform.ingestor.services

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{IdentifierSchemes, Period, SourceIdentifier, Work}
import uk.ac.wellcome.platform.ingestor.test.utils.Ingestor
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class WorkIndexerTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with MockitoSugar
    with Ingestor {

  val metricsSender: MetricsSender =
    new MetricsSender(namespace = "reindexer-tests", mock[AmazonCloudWatch])

  val workIndexer =
    new WorkIndexer(indexName, itemType, elasticClient, metricsSender)

  it("should insert an identified Work into Elasticsearch") {
    val work = createWork("5678", "1234", "An identified igloo")
    val future = workIndexer.indexWork(work)

    whenReady(future) { _ =>
      assertElasticsearchEventuallyHasWork(expectedWork)
    }
  }

  it(
    "should add only one record when multiple records with same id are ingested") {
    val work = createWork("5678", "1234", "A multiplicity of mice")

    val future = Future.sequence(
      (1 to 2).map(_ => workIndexer.indexWork(work))
    )

    whenReady(future) { _ =>
      assertElasticsearchEventuallyHasWork(work)
    }
  }

  it("should insert a stub record if the Work has visible = false") {
    val sourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.miroImageNumber,
      value = "J0001234"
    )

    // We deliberately populate lots of fields here, and check that none
    // of them appear in Elasticsearch.
    val work = Work(
      canonicalId = Some("ju5ys4tj"),
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier),
      title = "Jumpin' Jupiter! The jackals had a jolt",
      description = Some("Jovial jaguars in Japan"),
      createdDate = Some(Period(label = "January 2018")),
      visible = false
    )

    val expectedWork = Work(
      canonicalId = work.canonicalId,
      sourceIdentifier = work.sourceIdentifier,
      identifiers = work.identifiers,
      title = "This work has been deleted",
      visible = false
    )

    val future = workIndexer.indexWork(work)

    whenReady(future) { _ =>
      assertElasticsearchEventuallyHasWork(expectedWork)
    }
  }

  it("should overwrite an existing record with a stub if it receives an update with visible = false") {
    val work = createWork(
      canonicalId = "kxdg3wmj",
      sourceId = "K0000567",
      title = "A kangaroo kicks a khaki kiln",
      visible = true
    )

    // Wait for the initial Work to be indexed into Elasticsearch correctly.
    whenReady(workIndexer.indexWork(work)) { _ =>
      assertElasticsearchEventuallyHasWork(work)
    }

    // Now we index the same work, but with visible = false.
    val expectedWork = work.copy(
      title = "This work has been deleted",
      visible = false
    )

    whenReady(workIndexer.indexWork(work.copy(visible = false))) { _ =>
      assertElasticsearchEventuallyHasWork(expectedWork)
    }
  }
}
