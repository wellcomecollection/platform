package uk.ac.wellcome.platform.ingestor.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.Subject
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WorkIndexerTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with ElasticsearchFixtures
    with WorksGenerators
    with CustomElasticsearchMapping {

  it("inserts an identified Work into Elasticsearch") {
    val work = createIdentifiedWork

    withLocalElasticsearchIndex { indexName =>
      withWorkIndexer { workIndexer =>
        val future = workIndexer.indexWorks(
          works = List(work),
          indexName = indexName,
          documentType = documentType
        )

        whenReady(future) { result =>
          result.right.get should contain(work)
          assertElasticsearchEventuallyHasWork(indexName = indexName, work)
        }
      }
    }
  }

  it("only adds one record when the same ID is ingested multiple times") {
    val work = createIdentifiedWork

    withLocalElasticsearchIndex { indexName =>
      withWorkIndexer { workIndexer =>
        val future = Future.sequence(
          (1 to 2).map(
            _ =>
              workIndexer.indexWorks(
                works = List(work),
                indexName = indexName,
                documentType = documentType
            ))
        )

        whenReady(future) { _ =>
          assertElasticsearchEventuallyHasWork(indexName = indexName, work)
        }
      }
    }
  }

  it("doesn't add a Work with a lower version") {
    val work = createIdentifiedWorkWith(version = 3)
    val olderWork = work.copy(version = 1)

    withLocalElasticsearchIndex { indexName =>
      insertIntoElasticsearch(indexName = indexName, work)

      withWorkIndexer { workIndexer =>
        val future = workIndexer.indexWorks(
          works = List(olderWork),
          indexName = indexName,
          documentType = documentType
        )

        whenReady(future) { result =>
          // Give Elasticsearch enough time to ingest the work
          Thread.sleep(700)
          result.right.get should contain(olderWork)

          assertElasticsearchEventuallyHasWork(indexName = indexName, work)
        }
      }
    }
  }

  it("replaces a Work with the same version") {
    val work = createIdentifiedWorkWith(version = 3)
    val updatedWork = work.copy(title = "a different title")

    withLocalElasticsearchIndex { indexName =>
      insertIntoElasticsearch(indexName = indexName, work)

      withWorkIndexer { workIndexer =>
        val future = workIndexer.indexWorks(
          works = List(updatedWork),
          indexName = indexName,
          documentType = documentType
        )

        whenReady(future) { result =>
          result.right.get should contain(updatedWork)
          assertElasticsearchEventuallyHasWork(
            indexName = indexName,
            updatedWork)
        }
      }
    }
  }

  it("inserts a list of works into elasticsearch and returns them") {
    val works = createIdentifiedWorks(count = 5)

    withLocalElasticsearchIndex { indexName =>
      withWorkIndexer { workIndexer =>
        val future = workIndexer.indexWorks(
          works = works,
          indexName = indexName,
          documentType = documentType
        )

        whenReady(future) { successfullyInserted =>
          assertElasticsearchEventuallyHasWork(indexName = indexName, works: _*)
          successfullyInserted.right.get should contain theSameElementsAs works
        }
      }
    }
  }

  it("returns a list of Works that weren't indexed correctly") {
    val subsetOfFieldsIndex = new OnlyInvisibleWorksIndex(
      elasticClient = elasticClient,
      documentType = documentType
    )

    val validWorks = createIdentifiedInvisibleWorks(count = 5)
    val notMatchingMappingWork = createIdentifiedWorkWith(
      subjects = List(Subject(label = "crystallography", concepts = Nil))
    )

    val works = validWorks :+ notMatchingMappingWork

    withLocalElasticsearchIndex(subsetOfFieldsIndex) { indexName =>
      withWorkIndexer { workIndexer =>
        val future = workIndexer.indexWorks(
          works = works,
          indexName = indexName,
          documentType = documentType
        )

        whenReady(future) { result =>
          assertElasticsearchEventuallyHasWork(
            indexName = indexName,
            validWorks: _*)
          assertElasticsearchNeverHasWork(
            indexName = indexName,
            notMatchingMappingWork)
          result.left.get should contain(notMatchingMappingWork)
        }
      }
    }
  }

  private def withWorkIndexer[R](testWith: TestWith[WorkIndexer, R]): R = {
    val workIndexer = new WorkIndexer(elasticClient = elasticClient)
    testWith(workIndexer)
  }
}
