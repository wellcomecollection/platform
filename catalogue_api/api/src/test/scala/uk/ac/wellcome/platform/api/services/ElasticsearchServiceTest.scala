package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.SearchHit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models.WorksUtil
import uk.ac.wellcome.models.IdentifiedWork
import uk.ac.wellcome.platform.api.fixtures.ElasticsearchServiceFixture
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class ElasticsearchServiceTest
    extends FunSpec
    with ElasticsearchServiceFixture
    with Matchers
    with ScalaFutures
    with WorksUtil {

  val itemType = "work"

  describe("simpleStringQueryResults") {
    it("finds results for a simpleStringQuery search") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val work1 = workWith(
          canonicalId = "000Z",
          title = "Amid an Aegean"
        )
        val work2 = workWith(
          canonicalId = "000Y",
          title = "Before a Bengal"
        )
        val work3 = workWith(
          canonicalId = "000X",
          title = "Circling a Cheetah"
        )

        insertIntoElasticsearch(indexName, itemType, work1, work2, work3)

        withElasticSearchService(indexName = indexName, itemType = itemType) {
          searchService =>
            val searchResultFuture = searchService.simpleStringQueryResults(
              queryString = "Aegean",
              indexName = indexName
            )

            whenReady(searchResultFuture) { result =>
              result.hits should have size 1
              val returnedWorks = result.hits.hits
                .map { h: SearchHit =>
                  jsonToIdentifiedWork(h.sourceAsString)
                }
              returnedWorks.toList shouldBe List(work1)
            }
        }
      }
    }
  }

  describe("findResultById") {
    it("finds a result by ID") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val canonicalId = "000Z"

        val expectedWork = workWith(
          canonicalId = canonicalId,
          title = "Amid an Aegean"
        )

        insertIntoElasticsearch(indexName, itemType, expectedWork)

        withElasticSearchService(indexName = indexName, itemType = itemType) {
          searchService =>
            val searchResultFuture: Future[GetResponse] =
              searchService.findResultById(
                canonicalId = canonicalId,
                indexName = indexName
              )

            whenReady(searchResultFuture) { result =>
              val returnedWork = jsonToIdentifiedWork(result.sourceAsString)
              returnedWork shouldBe expectedWork
            }
        }
      }
    }
  }

  describe("listResults") {
    it("sorts results from Elasticsearch in the correct order") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val work1 = workWith(
          canonicalId = "000Z",
          title = "Amid an Aegean"
        )
        val work2 = workWith(
          canonicalId = "000Y",
          title = "Before a Bengal"
        )
        val work3 = workWith(
          canonicalId = "000X",
          title = "Circling a Cheetah"
        )

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
        val visibleWorks = createWorks(count = 8, visible = true)
        val invisibleWorks = createWorks(count = 2, start = 9, visible = false)

        val works = visibleWorks ++ invisibleWorks
        insertIntoElasticsearch(indexName, itemType, works: _*)

        assertSliceIsCorrect(
          indexName = indexName,
          limit = 10,
          from = 0,
          expectedWorks = works.toList
        )
      }
    }
  }

  private def populateElasticsearch(indexName: String): List[IdentifiedWork] = {
    val works = createWorks(10)

    insertIntoElasticsearch(indexName, itemType, works: _*)

    works.sortBy(_.canonicalId).toList
  }

  private def assertSliceIsCorrect(
    indexName: String,
    limit: Int,
    from: Int,
    expectedWorks: List[IdentifiedWork]
  ) = {
    withElasticSearchService(indexName = indexName, itemType = itemType) {
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
              jsonToIdentifiedWork(h.sourceAsString)
            }
          returnedWorks.toList shouldBe expectedWorks
        }
    }
  }

  private def jsonToIdentifiedWork(document: String): IdentifiedWork =
    fromJson[IdentifiedWork](document).get
}
