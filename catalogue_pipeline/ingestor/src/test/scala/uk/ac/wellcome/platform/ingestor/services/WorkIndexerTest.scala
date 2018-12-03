package uk.ac.wellcome.platform.ingestor.services

import com.sksamuel.elastic4s.Index
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

    withLocalWorksIndex2 { index: Index =>
      withWorkIndexer { workIndexer =>
        val future = indexWork(work, index, workIndexer)

        whenReady(future) { result =>
          result.right.get should contain(work)
          assertElasticsearchEventuallyHasWork2(index = index, work)
        }
      }
    }
  }

  it("only adds one record when the same ID is ingested multiple times") {
    val work = createIdentifiedWork

    withLocalWorksIndex2 { index: Index =>
      withWorkIndexer { workIndexer =>
        val future = Future.sequence(
          (1 to 2).map(_ => indexWork(work, index, workIndexer))
        )

        whenReady(future) { _ =>
          assertElasticsearchEventuallyHasWork2(index = index, work)
        }
      }
    }
  }

  it("doesn't add a Work with a lower version") {
    val work = createIdentifiedWorkWith(version = 3)
    val olderWork = work.copy(version = 1)

    withLocalWorksIndex2 { index: Index =>
      withWorkIndexer { workIndexer =>
        val future = ingestWorkPairInOrder(
          firstWork = work,
          secondWork = olderWork,
          index = index,
          workIndexer = workIndexer)

        whenReady(future) { result =>
          assertIngestedWorkIs(
            result = result,
            ingestedWork = work,
            index = index)
        }
      }
    }
  }

  it("replaces a Work with the same version") {
    val work = createIdentifiedWorkWith(version = 3)
    val updatedWork = work.copy(title = "a different title")

    withLocalWorksIndex2 { index: Index =>
      withWorkIndexer { workIndexer =>
        val future = ingestWorkPairInOrder(
          firstWork = work,
          secondWork = updatedWork,
          index = index,
          workIndexer = workIndexer)

        whenReady(future) { result =>
          result.right.get should contain(updatedWork)
          assertElasticsearchEventuallyHasWork2(
            index = index,
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

      withLocalWorksIndex2 { index: Index =>
        withWorkIndexer { workIndexer =>
          val unmergedWorkInsertFuture = ingestWorkPairInOrder(
            firstWork = mergedWork,
            secondWork = unmergedWork,
            index = index,
            workIndexer = workIndexer)

          whenReady(unmergedWorkInsertFuture) { result =>
            assertIngestedWorkIs(
              result = result,
              ingestedWork = mergedWork,
              index = index)
          }
        }
      }
    }
    it("doesn't overwrite a Work with lower version and merged = true") {
      val unmergedNewWork = createIdentifiedWorkWith(version = 4)
      val mergedOldWork = unmergedNewWork.copy(version = 3, merged = true)

      withLocalWorksIndex2 { index: Index =>
        withWorkIndexer { workIndexer =>
          val mergedWorkInsertFuture = ingestWorkPairInOrder(
            firstWork = unmergedNewWork,
            secondWork = mergedOldWork,
            index = index,
            workIndexer = workIndexer)
          whenReady(mergedWorkInsertFuture) { result =>
            assertIngestedWorkIs(
              result = result,
              ingestedWork = unmergedNewWork,
              index = index)
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

      withLocalWorksIndex2 { index: Index =>
        withWorkIndexer { workIndexer =>
          val redirectedWorkInsertFuture = ingestWorkPairInOrder(
            firstWork = identifiedNewWork,
            secondWork = redirectedOldWork,
            index = index,
            workIndexer = workIndexer)
          whenReady(redirectedWorkInsertFuture) { result =>
            assertIngestedWorkIs(
              result = result,
              ingestedWork = identifiedNewWork,
              index = index)
          }
        }
      }
    }
    it("doesn't override a redirected Work with identified work same version") {
      val redirectedWork = createIdentifiedRedirectedWorkWith(version = 3)
      val identifiedWork = createIdentifiedWorkWith(
        canonicalId = redirectedWork.canonicalId,
        version = 3)

      withLocalWorksIndex2 { index: Index =>
        withWorkIndexer { workIndexer =>
          val identifiedWorkInsertFuture = ingestWorkPairInOrder(
            firstWork = redirectedWork,
            secondWork = identifiedWork,
            index = index,
            workIndexer = workIndexer)
          whenReady(identifiedWorkInsertFuture) { result =>
            assertIngestedWorkIs(
              result = result,
              ingestedWork = redirectedWork,
              index = index)
          }
        }
      }
    }
    it("overrides a identified Work with invisible work with higher version") {
      val work = createIdentifiedWorkWith(version = 3)
      val invisibleWork = createIdentifiedInvisibleWorkWith(
        canonicalId = work.canonicalId,
        version = 4)

      withLocalWorksIndex2 { index: Index =>
        withWorkIndexer { workIndexer =>
          val invisibleWorkInsertFuture = ingestWorkPairInOrder(
            firstWork = work,
            secondWork = invisibleWork,
            index = index,
            workIndexer = workIndexer)
          whenReady(invisibleWorkInsertFuture) { result =>
            assertIngestedWorkIs(
              result = result,
              ingestedWork = invisibleWork,
              index = index)
          }
        }
      }
    }

  }

  it("inserts a list of works into elasticsearch and returns them") {
    val works = createIdentifiedWorks(count = 5)

    withLocalWorksIndex2 { index: Index =>
      withWorkIndexer { workIndexer =>
        val future = workIndexer.indexWorks(
          works = works,
          index = index,
          documentType = documentType
        )

        whenReady(future) { successfullyInserted =>
          assertElasticsearchEventuallyHasWork2(index = index, works: _*)
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

    withLocalElasticsearchIndex2(OnlyInvisibleWorksIndex) { index: Index =>
      withWorkIndexer { workIndexer =>
        val future = workIndexer.indexWorks(
          works = works,
          index = index,
          documentType = documentType
        )

        whenReady(future) { result =>
          assertElasticsearchEventuallyHasWork2(
            index = index,
            validWorks: _*)
          assertElasticsearchNeverHasWork2(
            index = index,
            notMatchingMappingWork)
          result.left.get should contain(notMatchingMappingWork)
        }
      }
    }
  }

  // TODO: Why not just create a single instance?
  private def withWorkIndexer[R](testWith: TestWith[WorkIndexer, R]): R = {
    val workIndexer = new WorkIndexer(elasticClient = elasticClient)
    testWith(workIndexer)
  }

  private def ingestWorkPairInOrder(firstWork: IdentifiedBaseWork,
                                    secondWork: IdentifiedBaseWork,
                                    index: Index,
                                    workIndexer: WorkIndexer) = {
    for {
      _ <- indexWork(firstWork, index, workIndexer)
      result <- indexWork(secondWork, index, workIndexer)
    } yield result
  }

  private def indexWork(work: IdentifiedBaseWork,
                        index: Index,
                        workIndexer: WorkIndexer) =
    workIndexer.indexWorks(
      works = List(work),
      index = index,
      documentType = documentType
    )

  private def assertIngestedWorkIs(
    result: Either[Seq[IdentifiedBaseWork], Seq[IdentifiedBaseWork]],
    ingestedWork: IdentifiedBaseWork,
    index: Index) = {
    result.isRight shouldBe true
    assertElasticsearchEventuallyHasWork2(index = index, ingestedWork)
  }
}
