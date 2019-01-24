package uk.ac.wellcome.platform.ingestor.services

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.http.ElasticDsl.{
  intField,
  keywordField,
  objectField
}
import com.sksamuel.elastic4s.mappings.FieldDefinition
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.{
  IdentifiedBaseWork,
  Subject,
  Unidentifiable
}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WorkIndexerTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with ElasticsearchFixtures
    with WorksGenerators {

  val workIndexer = new WorkIndexer(elasticClient = elasticClient)

  it("inserts an identified Work into Elasticsearch") {
    val work = createIdentifiedWork

    withLocalWorksIndex { index =>
      val future = indexWork(work, index)

      whenReady(future) { result =>
        result.right.get should contain(work)
        assertElasticsearchEventuallyHasWork(index = index, work)
      }
    }
  }

  it("only adds one record when the same ID is ingested multiple times") {
    val work = createIdentifiedWork

    withLocalWorksIndex { index =>
      val future = Future.sequence(
        (1 to 2).map(_ => indexWork(work, index))
      )

      whenReady(future) { _ =>
        assertElasticsearchEventuallyHasWork(index = index, work)
      }
    }
  }

  it("doesn't add a Work with a lower version") {
    val work = createIdentifiedWorkWith(version = 3)
    val olderWork = work.copy(version = 1)

    withLocalWorksIndex { index =>
      val future = ingestWorkPairInOrder(
        firstWork = work,
        secondWork = olderWork,
        index = index
      )

      whenReady(future) { result =>
        assertIngestedWorkIs(
          result = result,
          ingestedWork = work,
          index = index)
      }
    }
  }

  it("replaces a Work with the same version") {
    val work = createIdentifiedWorkWith(version = 3)
    val updatedWork = work.copy(title = "a different title")

    withLocalWorksIndex { index =>
      val future = ingestWorkPairInOrder(
        firstWork = work,
        secondWork = updatedWork,
        index = index
      )

      whenReady(future) { result =>
        result.right.get should contain(updatedWork)
        assertElasticsearchEventuallyHasWork(index = index, updatedWork)
      }
    }
  }

  describe("updating merged / redirected works") {
    it(
      "doesn't override a merged Work with same version but merged flag = false") {
      val mergedWork = createIdentifiedWorkWith(version = 3, merged = true)
      val unmergedWork = mergedWork.copy(merged = false)

      withLocalWorksIndex { index =>
        val unmergedWorkInsertFuture = ingestWorkPairInOrder(
          firstWork = mergedWork,
          secondWork = unmergedWork,
          index = index
        )

        whenReady(unmergedWorkInsertFuture) { result =>
          assertIngestedWorkIs(
            result = result,
            ingestedWork = mergedWork,
            index = index)
        }
      }
    }

    it("doesn't overwrite a Work with lower version and merged = true") {
      val unmergedNewWork = createIdentifiedWorkWith(version = 4)
      val mergedOldWork = unmergedNewWork.copy(version = 3, merged = true)

      withLocalWorksIndex { index =>
        val mergedWorkInsertFuture = ingestWorkPairInOrder(
          firstWork = unmergedNewWork,
          secondWork = mergedOldWork,
          index = index
        )
        whenReady(mergedWorkInsertFuture) { result =>
          assertIngestedWorkIs(
            result = result,
            ingestedWork = unmergedNewWork,
            index = index)
        }
      }
    }

    it(
      "doesn't override a identified Work with redirected work with lower version") {
      val identifiedNewWork = createIdentifiedWorkWith(version = 4)
      val redirectedOldWork = createIdentifiedRedirectedWorkWith(
        canonicalId = identifiedNewWork.canonicalId,
        version = 3)

      withLocalWorksIndex { index =>
        val redirectedWorkInsertFuture = ingestWorkPairInOrder(
          firstWork = identifiedNewWork,
          secondWork = redirectedOldWork,
          index = index
        )
        whenReady(redirectedWorkInsertFuture) { result =>
          assertIngestedWorkIs(
            result = result,
            ingestedWork = identifiedNewWork,
            index = index)
        }
      }
    }

    it("doesn't override a redirected Work with identified work same version") {
      val redirectedWork = createIdentifiedRedirectedWorkWith(version = 3)
      val identifiedWork = createIdentifiedWorkWith(
        canonicalId = redirectedWork.canonicalId,
        version = 3)

      withLocalWorksIndex { index =>
        val identifiedWorkInsertFuture = ingestWorkPairInOrder(
          firstWork = redirectedWork,
          secondWork = identifiedWork,
          index = index
        )
        whenReady(identifiedWorkInsertFuture) { result =>
          assertIngestedWorkIs(
            result = result,
            ingestedWork = redirectedWork,
            index = index)
        }
      }
    }

    it("overrides a identified Work with invisible work with higher version") {
      val work = createIdentifiedWorkWith(version = 3)
      val invisibleWork = createIdentifiedInvisibleWorkWith(
        canonicalId = work.canonicalId,
        version = 4)

      withLocalWorksIndex { index =>
        val invisibleWorkInsertFuture = ingestWorkPairInOrder(
          firstWork = work,
          secondWork = invisibleWork,
          index = index
        )
        whenReady(invisibleWorkInsertFuture) { result =>
          assertIngestedWorkIs(
            result = result,
            ingestedWork = invisibleWork,
            index = index)
        }
      }
    }
  }

  it("inserts a list of works into elasticsearch and returns them") {
    val works = createIdentifiedWorks(count = 5)

    withLocalWorksIndex { index =>
      val future = workIndexer.indexWorks(
        works = works,
        index = index
      )

      whenReady(future) { successfullyInserted =>
        assertElasticsearchEventuallyHasWork(index = index, works: _*)
        successfullyInserted.right.get should contain theSameElementsAs works
      }
    }
  }

  object OnlyInvisibleWorksIndex {
    def sourceIdentifierFields = Seq(
      keywordField("ontologyType"),
      objectField("identifierType").fields(
        keywordField("id"),
        keywordField("label"),
        keywordField("ontologyType")
      ),
      keywordField("value")
    )

    val rootIndexFields: Seq[FieldDefinition with Product with Serializable] =
      Seq(
        keywordField("canonicalId"),
        intField("version"),
        objectField("sourceIdentifier")
          .fields(sourceIdentifierFields),
        keywordField("type")
      )
  }

  it("returns a list of Works that weren't indexed correctly") {
    val validWorks = createIdentifiedInvisibleWorks(count = 5)
    val notMatchingMappingWork = createIdentifiedWorkWith(
      subjects = List(
        Unidentifiable(
          Subject(label = "crystallography", concepts = Nil)
        )
      )
    )

    val works = validWorks :+ notMatchingMappingWork

    withLocalElasticsearchIndex(
      fields = OnlyInvisibleWorksIndex.rootIndexFields) { index =>
      val future = workIndexer.indexWorks(
        works = works,
        index = index
      )

      whenReady(future) { result =>
        assertElasticsearchEventuallyHasWork(index = index, validWorks: _*)
        assertElasticsearchNeverHasWork(index = index, notMatchingMappingWork)
        result.left.get should contain(notMatchingMappingWork)
      }
    }
  }

  private def ingestWorkPairInOrder(firstWork: IdentifiedBaseWork,
                                    secondWork: IdentifiedBaseWork,
                                    index: Index) =
    for {
      _ <- indexWork(firstWork, index)
      result <- indexWork(secondWork, index)
    } yield result

  private def indexWork(work: IdentifiedBaseWork, index: Index) =
    workIndexer.indexWorks(
      works = List(work),
      index = index
    )

  private def assertIngestedWorkIs(
    result: Either[Seq[IdentifiedBaseWork], Seq[IdentifiedBaseWork]],
    ingestedWork: IdentifiedBaseWork,
    index: Index): Seq[Assertion] = {
    result.isRight shouldBe true
    assertElasticsearchEventuallyHasWork(index = index, ingestedWork)
  }
}
