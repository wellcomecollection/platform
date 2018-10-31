package uk.ac.wellcome.platform.ingestor.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.Subject
import uk.ac.wellcome.platform.ingestor.fixtures.WorkIndexerFixtures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class WorkIndexerTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with ElasticsearchFixtures
    with WorksGenerators
    with WorkIndexerFixtures
    with CustomElasticsearchMapping {

  val esType = "work"

  it("inserts an identified Work into Elasticsearch") {
    val work = createIdentifiedWork

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWorks(List(work), indexName, esType)

        whenReady(future) { result =>
          result.right.get should contain(work)
          assertElasticsearchEventuallyHasWork(
            indexName = indexName,
            itemType = esType,
            work)
        }
      }
    }
  }

  it("only adds one record when the same ID is ingested multiple times") {
    val work = createIdentifiedWork

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
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
          assertElasticsearchEventuallyHasWork(
            indexName = indexName,
            itemType = esType,
            work)
        }
      }
    }
  }

  it("doesn't add a Work with a lower version") {
    val work = createIdentifiedWorkWith(version = 3)
    val olderWork = work.copy(version = 1)

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      insertIntoElasticsearch(indexName = indexName, itemType = esType, work)

      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWorks(
          works = List(olderWork),
          esIndex = indexName,
          esType = esType
        )

        whenReady(future) { result =>
          // Give Elasticsearch enough time to ingest the work
          Thread.sleep(700)
          result.right.get should contain(olderWork)

          assertElasticsearchEventuallyHasWork(
            indexName = indexName,
            itemType = esType,
            work)
        }
      }
    }
  }

  it("replaces a Work with the same version") {
    val work = createIdentifiedWorkWith(version = 3)
    val updatedWork = work.copy(title = "a different title")

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      insertIntoElasticsearch(indexName = indexName, itemType = esType, work)

      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWorks(
          works = List(updatedWork),
          esIndex = indexName,
          esType = esType
        )

        whenReady(future) { result =>
          result.right.get should contain(updatedWork)
          assertElasticsearchEventuallyHasWork(
            indexName = indexName,
            itemType = esType,
            updatedWork)
        }
      }
    }
  }

  it("inserts a list of works into elasticsearch and returns them") {
    val works = createIdentifiedWorks(count = 5)

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWorks(works, indexName, esType)

        whenReady(future) { successfullyInserted =>
          assertElasticsearchEventuallyHasWork(
            indexName = indexName,
            itemType = esType,
            works: _*)
          successfullyInserted.right.get should contain theSameElementsAs works
        }
      }
    }
  }

  it(
    "inserts a list of works into elasticsearch and return the list of works that failed inserting") {
    val subsetOfFieldsIndex =
      new SubsetOfFieldsWorksIndex(elasticClient, esType)

    val validWorks = createIdentifiedWorks(count = 5)
    val notMatchingMappingWork = createIdentifiedWorkWith(
      subjects = List(Subject(label = "crystallography", concepts = Nil))
    )

    val works = validWorks :+ notMatchingMappingWork

    withLocalElasticsearchIndex(
      subsetOfFieldsIndex,
      indexName = (Random.alphanumeric take 10 mkString) toLowerCase) {
      indexName =>
        withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
          val future = workIndexer.indexWorks(works, indexName, esType)

          whenReady(future) { result =>
            assertElasticsearchEventuallyHasWork(
              indexName = indexName,
              itemType = esType,
              validWorks: _*)
            assertElasticsearchNeverHasWork(
              indexName = indexName,
              itemType = esType,
              notMatchingMappingWork)
            result.left.get should contain(notMatchingMappingWork)
          }
        }
    }
  }
}
