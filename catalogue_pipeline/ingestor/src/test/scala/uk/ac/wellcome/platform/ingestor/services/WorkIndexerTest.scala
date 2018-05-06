package uk.ac.wellcome.platform.ingestor.services

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.concurrent.duration._

class WorkIndexerTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with MockitoSugar
    with ElasticsearchFixtures
    with WorksUtil {

  val metricsSender: MetricsSender =
    new MetricsSender(
      namespace = "reindexer-tests",
      100 milliseconds,
      mock[AmazonCloudWatch],
      ActorSystem())

  val itemType = "work"

  val workIndexer =
    new WorkIndexer(itemType, elasticClient, metricsSender)

  it("inserts an identified Work into Elasticsearch") {
    val work = createVersionedWork()

    withLocalElasticsearchIndex(itemType) { indexName =>
      val future = workIndexer.indexWork(work, indexName)

      whenReady(future) { _ =>
        assertElasticsearchEventuallyHasWork(
          work,
          indexName = indexName,
          itemType = itemType)
      }
    }
  }

  it("only adds one record when the same ID is ingested multiple times") {
    val work = createVersionedWork()

    withLocalElasticsearchIndex(itemType) { indexName =>
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

  it("doesn't add a Work with a lower version") {
    val work = createVersionedWork(version = 3)
    val olderWork = work.copy(version = 1)

    withLocalElasticsearchIndex(itemType) { indexName =>
      insertIntoElasticsearch(indexName = indexName, itemType = itemType, work)

      val future = workIndexer.indexWork(olderWork, indexName)

      whenReady(future) { _ =>
        // Give Elasticsearch enough time to ingest the work
        Thread.sleep(700)

        assertElasticsearchEventuallyHasWork(
          work,
          indexName = indexName,
          itemType = itemType)
      }
    }
  }

  it("replaces a Work with the same version") {
    val work = createVersionedWork(version = 3)
    val updatedWork = work.copy(title = Some("boring title"))

    withLocalElasticsearchIndex(itemType) { indexName =>
      insertIntoElasticsearch(indexName = indexName, itemType = itemType, work)

      val future = workIndexer.indexWork(updatedWork, indexName)

      whenReady(future) { _ =>
        assertElasticsearchEventuallyHasWork(
          updatedWork,
          indexName = indexName,
          itemType = itemType)
      }
    }
  }

  def createVersionedWork(version: Int = 1): IdentifiedWork =
    createWorks(count = 1)
      .head
      .copy(version = version)
}
