package uk.ac.wellcome.platform.ingestor.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.{IdentifiedBaseWork, Subject}
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

    withLocalWorksIndex { indexName =>
      withWorkIndexer { workIndexer =>
        val future = indexWork(work, indexName, workIndexer)

        whenReady(future) { result =>
          result.right.get should contain(work)
          assertElasticsearchEventuallyHasWork(indexName = indexName, work)
        }
      }
    }
  }

  it("only adds one record when the same ID is ingested multiple times") {
    val work = createIdentifiedWork

    withLocalWorksIndex { indexName =>
      withWorkIndexer { workIndexer =>
        val future = Future.sequence(
          (1 to 2).map(_ => indexWork(work, indexName, workIndexer))
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

    withLocalWorksIndex { indexName =>
      withWorkIndexer { workIndexer =>
        val future = ingestWorkPairInOrder(
          firstWork = work,
          secondWork = olderWork,
          indexName = indexName,
          workIndexer = workIndexer)

        whenReady(future) { result =>
          assertIngestedWorkIs(
            result = result,
            ingestedWork = work,
            indexName = indexName)
        }
      }
    }
  }

  it("replaces a Work with the same version") {
    val work = createIdentifiedWorkWith(version = 3)
    val updatedWork = work.copy(title = "a different title")

    withLocalWorksIndex { indexName =>
      withWorkIndexer { workIndexer =>
        val future = ingestWorkPairInOrder(
          firstWork = work,
          secondWork = updatedWork,
          indexName = indexName,
          workIndexer = workIndexer)

        whenReady(future) { result =>
          result.right.get should contain(updatedWork)
          assertElasticsearchEventuallyHasWork(
            indexName = indexName,
            updatedWork)
        }
      }
    }
  }

  describe("updating merged / redirected works") {
    it(
      "doesn't override a merged Work with same version but merged flag = false") {
      val mergedWork = createIdentifiedWorkWith(version = 3, merged = true)
      val unmergedWork = mergedWork.copy(merged = false)

      withLocalWorksIndex { indexName =>
        withWorkIndexer { workIndexer =>
          val unmergedWorkInsertFuture = ingestWorkPairInOrder(
            firstWork = mergedWork,
            secondWork = unmergedWork,
            indexName = indexName,
            workIndexer = workIndexer)

          whenReady(unmergedWorkInsertFuture) { result =>
            assertIngestedWorkIs(
              result = result,
              ingestedWork = mergedWork,
              indexName = indexName)
          }
        }
      }
    }
    it("doesn't overwrite a Work with lower version and merged = true") {
      val unmergedNewWork = createIdentifiedWorkWith(version = 4)
      val mergedOldWork = unmergedNewWork.copy(version = 3, merged = true)

      withLocalWorksIndex { indexName =>
        withWorkIndexer { workIndexer =>
          val mergedWorkInsertFuture = ingestWorkPairInOrder(
            firstWork = unmergedNewWork,
            secondWork = mergedOldWork,
            indexName = indexName,
            workIndexer = workIndexer)
          whenReady(mergedWorkInsertFuture) { result =>
            assertIngestedWorkIs(
              result = result,
              ingestedWork = unmergedNewWork,
              indexName = indexName)
          }
        }
      }
    }

    it(
      "doesn't override a identified Work with redirected work with lower version") {
      val identifiedNewWork = createIdentifiedWorkWith(version = 4)
      val redirectedOldWork = createIdentifiedRedirectedWorkWith(
        canonicalId = identifiedNewWork.canonicalId,
        version = 3)

      withLocalWorksIndex { indexName =>
        withWorkIndexer { workIndexer =>
          val redirectedWorkInsertFuture = ingestWorkPairInOrder(
            firstWork = identifiedNewWork,
            secondWork = redirectedOldWork,
            indexName = indexName,
            workIndexer = workIndexer)
          whenReady(redirectedWorkInsertFuture) { result =>
            assertIngestedWorkIs(
              result = result,
              ingestedWork = identifiedNewWork,
              indexName = indexName)
          }
        }
      }
    }
    it("doesn't override a redirected Work with identified work same version") {
      val redirectedWork = createIdentifiedRedirectedWorkWith(version = 3)
      val identifiedWork = createIdentifiedWorkWith(
        canonicalId = redirectedWork.canonicalId,
        version = 3)

      withLocalWorksIndex { indexName =>
        withWorkIndexer { workIndexer =>
          val identifiedWorkInsertFuture = ingestWorkPairInOrder(
            firstWork = redirectedWork,
            secondWork = identifiedWork,
            indexName = indexName,
            workIndexer = workIndexer)
          whenReady(identifiedWorkInsertFuture) { result =>
            assertIngestedWorkIs(
              result = result,
              ingestedWork = redirectedWork,
              indexName = indexName)
          }
        }
      }
    }
    it("overrides a identified Work with invisible work with higher version") {
      val work = createIdentifiedWorkWith(version = 3)
      val invisibleWork = createIdentifiedInvisibleWorkWith(
        canonicalId = work.canonicalId,
        version = 4)

      withLocalWorksIndex { indexName =>
        withWorkIndexer { workIndexer =>
          val invisibleWorkInsertFuture = ingestWorkPairInOrder(
            firstWork = work,
            secondWork = invisibleWork,
            indexName = indexName,
            workIndexer = workIndexer)
          whenReady(invisibleWorkInsertFuture) { result =>
            assertIngestedWorkIs(
              result = result,
              ingestedWork = invisibleWork,
              indexName = indexName)
          }
        }
      }
    }

  }

  it("inserts a list of works into elasticsearch and returns them") {
    val works = createIdentifiedWorks(count = 5)

    withLocalWorksIndex { indexName =>
      withWorkIndexer { workIndexer =>
        val future = workIndexer.indexWorks(
          works = works,
          indexName = indexName
        )

        whenReady(future) { successfullyInserted =>
          assertElasticsearchEventuallyHasWork(indexName = indexName, works: _*)
          successfullyInserted.right.get should contain theSameElementsAs works
        }
      }
    }
  }

  it("returns a list of Works that weren't indexed correctly") {
    val validWorks = createIdentifiedInvisibleWorks(count = 5)
    val notMatchingMappingWork = createIdentifiedWorkWith(
      subjects = List(Subject(label = "crystallography", concepts = Nil))
    )

    val works = validWorks :+ notMatchingMappingWork

    withLocalElasticsearchIndex(OnlyInvisibleWorksIndex) { indexName =>
      withWorkIndexer { workIndexer =>
        val future = workIndexer.indexWorks(
          works = works,
          indexName = indexName
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

  private def ingestWorkPairInOrder(firstWork: IdentifiedBaseWork,
                                    secondWork: IdentifiedBaseWork,
                                    indexName: String,
                                    workIndexer: WorkIndexer) = {
    for {
      _ <- indexWork(firstWork, indexName, workIndexer)
      result <- indexWork(secondWork, indexName, workIndexer)
    } yield result
  }

  private def indexWork(work: IdentifiedBaseWork,
                        indexName: String,
                        workIndexer: WorkIndexer) = workIndexer.indexWorks(
    works = List(work),
    indexName = indexName
  )

  private def assertIngestedWorkIs(
    result: Either[Seq[IdentifiedBaseWork], Seq[IdentifiedBaseWork]],
    ingestedWork: IdentifiedBaseWork,
    indexName: String) = {
    result.isRight shouldBe true
    assertElasticsearchEventuallyHasWork(indexName = indexName, ingestedWork)
  }
}
