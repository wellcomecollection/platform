package uk.ac.wellcome.platform.ingestor.services

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.fasterxml.jackson.core.JsonParseException
import com.sksamuel.elastic4s.http.ElasticDsl._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{SourceIdentifier, Work}
import uk.ac.wellcome.test.utils.IndexedElasticSearchLocal
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

class WorkIndexerTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with MockitoSugar
    with IndexedElasticSearchLocal {

  val metricsSender: MetricsSender =
    new MetricsSender(namespace = "reindexer-tests", mock[AmazonCloudWatch])

  val workIndexer =
    new WorkIndexer(indexName,
                              itemType,
                              elasticClient,
                              metricsSender)

  def workJson(canonicalId: String,
               sourceId: String,
               title: String): String = {
    JsonUtil
      .toJson(
        Work(
            canonicalId = Some(canonicalId),
                identifiers =
                        List(SourceIdentifier(IdentifierSchemes.miroImageNumber, sourceId)),
                      title = title))
      .get
  }

  it("should insert an identified unified item into Elasticsearch") {

    val workString =
      workJson("5678", "1234", "An identified igloo")

    val future =
      workIndexer.indexWork(workString)

    whenReady(future) { _ =>
      eventually {
        val hits = elasticClient
          .execute(search(s"$indexName/$itemType").matchAllQuery().limit(100))
          .map { _.hits.hits }
          .await
        hits should have size 1
        hits.head.sourceAsString shouldBe workString
      }
    }

  }

  it(
    "should add only one record when multiple records with same id are ingested") {
    val workString =
      workJson("5678", "1234", "A multiplicity of mice")

    val future = Future.sequence(
      (1 to 2).map(_ =>
        workIndexer.indexWork(workString))
    )

    whenReady(future) { _ =>
      eventually {
        val hits = elasticClient
          .execute(search(s"$indexName/$itemType").matchAllQuery().limit(100))
          .map { _.hits.hits }
          .await
        hits should have size 1
        hits.head.sourceAsString shouldBe workString
      }
    }

  }

  it(
    "should return a failed future if the input string is not a Work") {
    val future = workIndexer.indexWork("a document")

    whenReady(future.failed) { exception =>
      exception shouldBe a[JsonParseException]
    }
  }
}
