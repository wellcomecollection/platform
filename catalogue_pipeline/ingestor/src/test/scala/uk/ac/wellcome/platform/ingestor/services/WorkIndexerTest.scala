package uk.ac.wellcome.platform.ingestor.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.platform.ingestor.fixtures.WorkIndexerFixtures
import uk.ac.wellcome.platform.ingestor.GlobalExecutionContext.context

import scala.concurrent.Future

class WorkIndexerTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with ElasticsearchFixtures
    with WorksUtil
    with WorkIndexerFixtures {

  val esType = "work"

  it("inserts an identified Work into Elasticsearch") {
    val work = createVersionedWork()

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWork(work, indexName, esType)

        whenReady(future) { _ =>
          assertElasticsearchEventuallyHasWork(
            work,
            indexName = indexName,
            itemType = esType)
        }
      }
    }
  }

  it("only adds one record when the same ID is ingested multiple times") {
    val work = createVersionedWork()

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      withWorkIndexerFixtures[Assertion](esType, elasticClient) {
        workIndexer =>
          val future = Future.sequence(
            (1 to 2).map(_ => workIndexer.indexWork(
              work = work,
              esIndex = indexName,
              esType = esType
            ))
          )

          whenReady(future) { _ =>
            assertElasticsearchEventuallyHasWork(
              work,
              indexName = indexName,
              itemType = esType)
          }
      }
    }
  }

  it("doesn't add a Work with a lower version") {
    val work = createVersionedWork(version = 3)
    val olderWork = work.copy(version = 1)

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      insertIntoElasticsearch(indexName = indexName, itemType = esType, work)

      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWork(olderWork, indexName)

        whenReady(future) { _ =>
          // Give Elasticsearch enough time to ingest the work
          Thread.sleep(700)

          assertElasticsearchEventuallyHasWork(
            work,
            indexName = indexName,
            itemType = esType)
        }
      }
    }
  }

  it("replaces a Work with the same version") {
    val work = createVersionedWork(version = 3)
    val updatedWork = work.copy(title = Some("boring title"))

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      insertIntoElasticsearch(indexName = indexName, itemType = esType, work)

      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWork(
          work = updatedWork,
          esIndex = indexName,
          esType = esType
        )

        whenReady(future) { _ =>
          assertElasticsearchEventuallyHasWork(
            updatedWork,
            indexName = indexName,
            itemType = esType)
        }
      }
    }
  }

  def createVersionedWork(version: Int = 1): IdentifiedWork =
    createWorks(count = 1).head
      .copy(version = version)
}
