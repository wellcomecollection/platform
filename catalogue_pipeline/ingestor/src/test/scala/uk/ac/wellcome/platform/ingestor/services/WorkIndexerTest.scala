package uk.ac.wellcome.platform.ingestor.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.platform.ingestor.fixtures.WorkIndexerFixtures

import scala.concurrent.ExecutionContext.Implicits.global
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
        val future = workIndexer.indexWorks(List(work), indexName, esType)

        whenReady(future) { _ =>
          assertElasticsearchEventuallyHasWork(indexName = indexName, itemType = esType, work)
        }
      }
    }
  }

  it("only adds one record when the same ID is ingested multiple times") {
    val work = createVersionedWork()

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      withWorkIndexerFixtures(esType, elasticClient) {
        workIndexer =>
          val future = Future.sequence(
            (1 to 2).map(
              _ =>
                workIndexer.indexWorks(
                  works = List(work),
                  esIndex = indexName,
                  esType = esType
              ))
          )

          whenReady(future) { _ =>
            assertElasticsearchEventuallyHasWork(indexName = indexName, itemType = esType, work)
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
        val future = workIndexer.indexWorks(
          works = List(olderWork),
          esIndex = indexName,
          esType = esType
        )

        whenReady(future) { _ =>
          // Give Elasticsearch enough time to ingest the work
          Thread.sleep(700)

          assertElasticsearchEventuallyHasWork(indexName = indexName, itemType = esType, work)
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
        val future = workIndexer.indexWorks(
          works = List(updatedWork),
          esIndex = indexName,
          esType = esType
        )

        whenReady(future) { _ =>
          assertElasticsearchEventuallyHasWork(indexName = indexName, itemType = esType, updatedWork)
        }
      }
    }
  }

  def createVersionedWork(version: Int = 1): IdentifiedWork =
    createWorks(count = 1).head
      .copy(version = version)
}
