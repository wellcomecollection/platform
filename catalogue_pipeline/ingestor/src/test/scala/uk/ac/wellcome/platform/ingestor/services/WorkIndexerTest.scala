package uk.ac.wellcome.platform.ingestor.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.platform.ingestor.fixtures.WorkIndexerFixture
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class WorkIndexerTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with ElasticsearchFixtures
    with WorksUtil {

  val esType = "work"

  it("inserts an identified Work into Elasticsearch") {
    val work = createVersionedWork()

    withLocalElasticsearchIndex(esType) { indexName =>
      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWork(work, indexName)

        whenReady(future) { _ =>
          assertElasticsearchEventuallyHasWork(
            work,
            indexName = indexName,
            esType = esType)
        }
      }
    }
  }

  it("only adds one record when the same ID is ingested multiple times") {
    val work = createVersionedWork()

    withLocalElasticsearchIndex(esType) { indexName =>
      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = Future.sequence(
          (1 to 2).map(_ => workIndexer.indexWork(work, indexName))
        )

        whenReady(future) { _ =>
          assertElasticsearchEventuallyHasWork(
            work,
            indexName = indexName,
            esType = esType)
        }
      }
    }
  }

  it("doesn't add a Work with a lower version") {
    val work = createVersionedWork(version = 3)
    val olderWork = work.copy(version = 1)

    withLocalElasticsearchIndex(esType) { indexName =>
      insertIntoElasticsearch(indexName = indexName, esType = esType, work)

      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWork(olderWork, indexName)

        whenReady(future) { _ =>
          // Give Elasticsearch enough time to ingest the work
          Thread.sleep(700)

          assertElasticsearchEventuallyHasWork(
            work,
            indexName = indexName,
            esType = esType)
        }
      }
    }
  }

  it("replaces a Work with the same version") {
    val work = createVersionedWork(version = 3)
    val updatedWork = work.copy(title = Some("boring title"))

    withLocalElasticsearchIndex(esType) { indexName =>
      insertIntoElasticsearch(indexName = indexName, esType = esType, work)

      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWork(updatedWork, indexName)

        whenReady(future) { _ =>
          assertElasticsearchEventuallyHasWork(
            updatedWork,
            indexName = indexName,
            esType = esType)
        }
      }
    }
  }

  def createVersionedWork(version: Int = 1): IdentifiedWork =
    createWorks(count = 1)
      .head
      .copy(version = version)
}
