package uk.ac.wellcome.platform.ingestor.services

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
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
    new MetricsSender(
      namespace = "reindexer-tests",
      mock[AmazonCloudWatch],
      ActorSystem())

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

  it("does not add a work with a lower version") {
    val work =
      createWork("5678", "1234", "A multiplicity of mice", version = 3)

    insertIntoElasticSearch(work)

    val future = workIndexer.indexWork(work.copy(version = 1))

    whenReady(future) { _ =>
      // give elasticsearch enough time to ingest the work
      Thread.sleep(700)

      assertElasticsearchEventuallyHasWork(work)
    }
  }

  it("replaces a work with the same version") {
    val work = createWork(
      canonicalId = "5678",
      sourceId = "1234",
      title = "A multiplicity of mice",
      version = 3)

    insertIntoElasticSearch(work)

    val updatedWork = work.copy(title = Some("boring title"))
    val future = workIndexer.indexWork(updatedWork)

    whenReady(future) { _ =>
      assertElasticsearchEventuallyHasWork(updatedWork)
    }
  }
}
