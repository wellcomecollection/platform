package uk.ac.wellcome.platform.ingestor.services

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.messaging.metrics.MetricsSender
import uk.ac.wellcome.models.work.internal.{
  IdentifiedWork,
  IdentifierSchemes,
  SourceIdentifier
}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.concurrent.duration._

class WorkIndexerTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with MockitoSugar
    with ElasticsearchFixtures {

  val metricsSender: MetricsSender =
    new MetricsSender(
      namespace = "reindexer-tests",
      100 milliseconds,
      mock[AmazonCloudWatch],
      ActorSystem())

  val indexName = "works"
  val itemType = "work"

  val workIndexer =
    new WorkIndexer(itemType, elasticClient, metricsSender)

  it("should insert an identified Work into Elasticsearch") {
    val work = createWork("5678", "1234", "An identified igloo")

    withLocalElasticsearchIndex(indexName, itemType) { _ =>
      val future = workIndexer.indexWork(work, indexName)

      whenReady(future) { _ =>
        assertElasticsearchEventuallyHasWork(
          work,
          indexName = indexName,
          itemType = itemType)
      }
    }
  }

  it(
    "should add only one record when multiple records with same id are ingested") {
    val work = createWork("5678", "1234", "A multiplicity of mice")

    withLocalElasticsearchIndex(indexName, itemType) { _ =>
      val future = Future.sequence(
        (1 to 2).map(_ => workIndexer.indexWork(work, indexName))
      )

      whenReady(future) { _ =>
        assertElasticsearchEventuallyHasWork(
          work,
          indexName = indexName,
          itemType = itemType)
      }
    }
  }

  it("does not add a work with a lower version") {
    val work =
      createWork("5678", "1234", "A multiplicity of mice", version = 3)

    withLocalElasticsearchIndex(indexName, itemType) { _ =>
      insertIntoElasticsearch(indexName = indexName, itemType = itemType, work)

      val future = workIndexer.indexWork(work.copy(version = 1), indexName)

      whenReady(future) { _ =>
        // give elasticsearch enough time to ingest the work
        Thread.sleep(700)

        assertElasticsearchEventuallyHasWork(
          work,
          indexName = indexName,
          itemType = itemType)
      }
    }
  }

  it("replaces a work with the same version") {
    val work = createWork(
      canonicalId = "5678",
      sourceId = "1234",
      title = "A multiplicity of mice",
      version = 3)

    withLocalElasticsearchIndex(indexName, itemType) { _ =>
      insertIntoElasticsearch(indexName = indexName, itemType = itemType, work)

      val updatedWork = work.copy(title = Some("boring title"))
      val future = workIndexer.indexWork(updatedWork, indexName)

      whenReady(future) { _ =>
        assertElasticsearchEventuallyHasWork(
          updatedWork,
          indexName = indexName,
          itemType = itemType)
      }
    }
  }

  def createWork(canonicalId: String,
                 sourceId: String,
                 title: String,
                 visible: Boolean = true,
                 version: Int = 1): IdentifiedWork = {
    val sourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.miroImageNumber,
      ontologyType = "Work",
      value = sourceId
    )

    IdentifiedWork(
      title = Some(title),
      sourceIdentifier = sourceIdentifier,
      version = version,
      identifiers = List(sourceIdentifier),
      canonicalId = canonicalId,
      visible = visible)
  }
}
