package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchResponse}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.{IdentifiedBaseWork, IdentifiedWork, WorkType}
import uk.ac.wellcome.models.work.test.util.WorksGenerators
import uk.ac.wellcome.platform.api.fixtures.ElasticsearchServiceFixture

import scala.concurrent.Future

class ElasticsearchServiceTest
    extends FunSpec
    with ElasticsearchServiceFixture
    with Matchers
    with ScalaFutures
    with WorksGenerators {

  val itemType = "work"

  describe("simpleStringQueryResults") {
    it("finds results for a simpleStringQuery search") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val work1 = createIdentifiedWorkWith(title = "Amid an Aegean")
        val work2 = createIdentifiedWorkWith(title = "Before a Bengal")
        val work3 = createIdentifiedWorkWith(title = "Circling a Cheetah")

        insertIntoElasticsearch(indexName, itemType, work1, work2, work3)

        withElasticsearchService(indexName = indexName, itemType = itemType) {
          searchService =>
            val searchResultFuture = searchService.simpleStringQueryResults(
              queryString = "Aegean",
              indexName = indexName
            )

            whenReady(searchResultFuture) { response =>
              searchResponseToWorks(response) shouldBe List(work1)
            }
        }
      }
    }

    it("finds results when filtering by workType") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val workWithCorrectWorkType = createIdentifiedWorkWith(
          title = "Animated artichokes", workType = Some(WorkType(id = "b", label = "Books"))
        )
        val workWithWrongTitle = createIdentifiedWorkWith(
          title = "Bouncing bananas", workType = Some(WorkType(id = "b", label = "Books"))
        )
        val workWithWrongWorkType = createIdentifiedWorkWith(
          title = "Animated artichokes", workType = Some(WorkType(id = "m", label = "Manuscripts"))
        )

        insertIntoElasticsearch(indexName, itemType, workWithCorrectWorkType, workWithWrongTitle, workWithWrongWorkType)

        withElasticsearchService(indexName = indexName, itemType = itemType) {
          searchService =>
            val searchResultFuture = searchService.simpleStringQueryResults(
              queryString = "artichokes",
              workType = Some("b"),
              indexName = indexName
            )

            whenReady(searchResultFuture) { response =>
              searchResponseToWorks(response) shouldBe List(workWithCorrectWorkType)
            }
        }
      }
    }
  }

  describe("findResultById") {
    it("finds a result by ID") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val work = createIdentifiedWork

        insertIntoElasticsearch(indexName, itemType, work)

        withElasticsearchService(indexName = indexName, itemType = itemType) {
          searchService =>
            val searchResultFuture: Future[GetResponse] =
              searchService.findResultById(
                canonicalId = work.canonicalId,
                indexName = indexName
              )

            whenReady(searchResultFuture) { result =>
              val returnedWork = jsonToIdentifiedBaseWork(result.sourceAsString)
              returnedWork shouldBe work
            }
        }
      }
    }
  }

  describe("listResults") {
    it("sorts results from Elasticsearch in the correct order") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val work1 = createIdentifiedWorkWith(canonicalId = "000Z")
        val work2 = createIdentifiedWorkWith(canonicalId = "000Y")
        val work3 = createIdentifiedWorkWith(canonicalId = "000X")

        insertIntoElasticsearch(indexName, itemType, work1, work2, work3)

        assertSliceIsCorrect(
          indexName = indexName,
          limit = 3,
          from = 0,
          expectedWorks = List(work3, work2, work1)
        )

      // TODO: canonicalID is the only user-defined field that we can sort on.
      // When we have other fields we can sort on, we should extend this test
      // for different sort orders.
      }
    }

    it("returns everything if we ask for a limit > result size") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val displayWorks = populateElasticsearch(indexName)

        assertSliceIsCorrect(
          indexName = indexName,
          limit = displayWorks.length + 1,
          from = 0,
          expectedWorks = displayWorks
        )
      }
    }

    it("returns a page from the beginning of the result set") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val displayWorks = populateElasticsearch(indexName)
        assertSliceIsCorrect(
          indexName = indexName,
          limit = 4,
          from = 0,
          expectedWorks = displayWorks.slice(0, 4)
        )
      }
    }

    it("returns a page from halfway through the result set") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val displayWorks = populateElasticsearch(indexName)
        assertSliceIsCorrect(
          indexName = indexName,
          limit = 4,
          from = 3,
          expectedWorks = displayWorks.slice(3, 7)
        )
      }
    }

    it("returns a page from the end of the result set") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val displayWorks = populateElasticsearch(indexName)
        assertSliceIsCorrect(
          indexName = indexName,
          limit = 7,
          from = 5,
          expectedWorks = displayWorks.slice(5, 10)
        )
      }
    }

    it("returns an empty page if asked for a limit > result size") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val displayWorks = populateElasticsearch(indexName)
        assertSliceIsCorrect(
          indexName = indexName,
          limit = 10,
          from = displayWorks.length * 2,
          expectedWorks = List()
        )
      }
    }

    it("does not list works that have visible=false") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val visibleWorks = createIdentifiedWorks(count = 8)
        val invisibleWorks = createIdentifiedInvisibleWorks(count = 2)

        val works = visibleWorks ++ invisibleWorks
        insertIntoElasticsearch(indexName, itemType, works: _*)

        assertSliceIsCorrect(
          indexName = indexName,
          limit = 10,
          from = 0,
          expectedWorks = visibleWorks.toList
        )
      }
    }
  }

  private def populateElasticsearch(indexName: String): List[IdentifiedWork] = {
    val works = createIdentifiedWorks(count = 10)

    insertIntoElasticsearch(indexName, itemType, works: _*)

    works.sortBy(_.canonicalId).toList
  }

  private def assertSliceIsCorrect(
    indexName: String,
    limit: Int,
    from: Int,
    expectedWorks: List[IdentifiedBaseWork]
  ) = {
    withElasticsearchService(indexName = indexName, itemType = itemType) {
      searchService =>
        val searchResultFuture = searchService.listResults(
          sortByField = "canonicalId",
          indexName = indexName,
          limit = limit,
          from = from
        )
        whenReady(searchResultFuture) { result =>
          result.hits should have size expectedWorks.length
          val returnedWorks = result.hits.hits
            .map { h: SearchHit =>
              jsonToIdentifiedBaseWork(h.sourceAsString)
            }
          returnedWorks.toList should contain theSameElementsAs expectedWorks
        }
    }
  }

  private def searchResponseToWorks(response: SearchResponse): List[IdentifiedBaseWork] =
    response
      .hits
      .hits
      .map { h: SearchHit => jsonToIdentifiedBaseWork(h.sourceAsString) }
      .toList

  private def jsonToIdentifiedBaseWork(document: String): IdentifiedBaseWork =
    fromJson[IdentifiedBaseWork](document).get
}
