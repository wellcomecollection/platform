package uk.ac.wellcome.platform.ingestor.services

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
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
      assertElasticsearchEventuallyHasWork(work)
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

  private def assertElasticsearchEventuallyHasWork(work: Work) = {
    val workJson = toJson(work).get

    eventually {
      val hits = elasticClient
        .execute(search(s"$indexName/$itemType").matchAllQuery().limit(100))
        .map { _.hits.hits }
        .await

      hits should have size 1

      assertJsonStringsAreEqual(hits.head.sourceAsString, workJson)
    }
  }

  private def createWork(canonicalId: String, sourceId: String, title: String): Work = {
    val sourceIdentifier = SourceIdentifier(
      IdentifierSchemes.miroImageNumber,
      sourceId
    )

    Work(
      canonicalId = Some(canonicalId),
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier),
      title = title
    )
  }
}
