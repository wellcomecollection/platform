package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.SearchHit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.{
  IdentifiedBaseWork,
  IdentifiedWork,
  WorkType
}
import uk.ac.wellcome.models.work.test.util.WorksGenerators
import uk.ac.wellcome.platform.api.fixtures.ElasticsearchServiceFixture
import uk.ac.wellcome.platform.api.models.ElasticsearchQueryOptions

import scala.concurrent.Future

class ElasticsearchServiceTest
    extends FunSpec
    with ElasticsearchServiceFixture
    with Matchers
    with ScalaFutures
    with WorksGenerators {

  val itemType = "work"

  describe("executeSearch") {
    it("finds results for a simpleStringQuery search") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val work1 = createIdentifiedWorkWith(title = "Amid an Aegean")
        val work2 = createIdentifiedWorkWith(title = "Before a Bengal")
        val work3 = createIdentifiedWorkWith(title = "Circling a Cheetah")

        insertIntoElasticsearch(indexName, itemType, work1, work2, work3)

        val queryOptions = ElasticsearchQueryOptions(
          queryString = Some("Aegean"),
          indexName = indexName
        )

        assertSearchReturnsExpectedWorks(
          queryOptions = queryOptions,
          expectedWorks = List(work1)
        )
      }
    }

    it("filters search results by workType") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val workWithCorrectWorkType = createIdentifiedWorkWith(
          title = "Animated artichokes",
          workType = Some(WorkType(id = "b", label = "Books"))
        )
        val workWithWrongTitle = createIdentifiedWorkWith(
          title = "Bouncing bananas",
          workType = Some(WorkType(id = "b", label = "Books"))
        )
        val workWithWrongWorkType = createIdentifiedWorkWith(
          title = "Animated artichokes",
          workType = Some(WorkType(id = "m", label = "Manuscripts"))
        )

        insertIntoElasticsearch(
          indexName,
          itemType,
          workWithCorrectWorkType,
          workWithWrongTitle,
          workWithWrongWorkType)

        val queryOptions = ElasticsearchQueryOptions(
          queryString = Some("artichokes"),
          workType = Some("b"),
          indexName = indexName
        )

        assertSearchReturnsExpectedWorks(
          queryOptions = queryOptions,
          expectedWorks = List(workWithCorrectWorkType)
        )
      }
    }

    it("sorts results from Elasticsearch in the correct order") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val work1 = createIdentifiedWorkWith(canonicalId = "000Z")
        val work2 = createIdentifiedWorkWith(canonicalId = "000Y")
        val work3 = createIdentifiedWorkWith(canonicalId = "000X")

        insertIntoElasticsearch(indexName, itemType, work1, work2, work3)

        val queryOptions = createListQueryOptions(indexName = indexName)

        assertSearchReturnsExpectedWorks(
          queryOptions = queryOptions,
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

        val queryOptions = createListQueryOptions(
          indexName = indexName,
          limit = displayWorks.length + 1
        )

        assertSearchReturnsExpectedWorks(
          queryOptions = queryOptions,
          expectedWorks = displayWorks
        )
      }
    }

    it("returns a page from the beginning of the result set") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val displayWorks = populateElasticsearch(indexName)

        val queryOptions = createListQueryOptions(
          indexName = indexName,
          limit = 4
        )

        assertSearchReturnsExpectedWorks(
          queryOptions = queryOptions,
          expectedWorks = displayWorks.slice(0, 4)
        )
      }
    }

    it("returns a page from halfway through the result set") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val displayWorks = populateElasticsearch(indexName)

        val queryOptions = createListQueryOptions(
          indexName = indexName,
          limit = 4,
          from = 3
        )

        assertSearchReturnsExpectedWorks(
          queryOptions = queryOptions,
          expectedWorks = displayWorks.slice(3, 7)
        )
      }
    }

    it("returns a page from the end of the result set") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val displayWorks = populateElasticsearch(indexName)

        val queryOptions = createListQueryOptions(
          indexName = indexName,
          from = 5
        )

        assertSearchReturnsExpectedWorks(
          queryOptions = queryOptions,
          expectedWorks = displayWorks.slice(5, 10)
        )
      }
    }

    it("returns an empty page if asked for a limit > result size") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val displayWorks = populateElasticsearch(indexName)

        val queryOptions = createListQueryOptions(
          indexName = indexName,
          from = displayWorks.length * 2
        )

        assertSearchReturnsExpectedWorks(
          queryOptions = queryOptions,
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

        val queryOptions = createListQueryOptions(indexName = indexName)

        assertSearchReturnsExpectedWorks(
          queryOptions = queryOptions,
          expectedWorks = visibleWorks.toList
        )
      }
    }

    it("filters list results by workType") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val work1 = createIdentifiedWorkWith(
          title = "Animated artichokes",
          workType = Some(WorkType(id = "b", label = "Books"))
        )
        val work2 = createIdentifiedWorkWith(
          title = "Bouncing bananas",
          workType = Some(WorkType(id = "b", label = "Books"))
        )
        val workWithWrongWorkType = createIdentifiedWorkWith(
          title = "Animated artichokes",
          workType = Some(WorkType(id = "m", label = "Manuscripts"))
        )

        insertIntoElasticsearch(
          indexName,
          itemType,
          work1,
          work2,
          workWithWrongWorkType)

        val queryOptions = createListQueryOptions(
          workType = Some("b"),
          indexName = indexName
        )

        assertSearchReturnsExpectedWorks(
          queryOptions = queryOptions,
          expectedWorks = List(work1, work2)
        )
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

  private def assertSearchReturnsExpectedWorks(
    queryOptions: ElasticsearchQueryOptions,
    expectedWorks: List[IdentifiedBaseWork]
  ) =
    withElasticsearchService(indexName = queryOptions.indexName, itemType = itemType) {
      searchService =>
        val searchResponseFuture = searchService.executeSearch(queryOptions)

        whenReady(searchResponseFuture) { response =>
          val actualWorks = response.hits.hits.map { h: SearchHit =>
            jsonToIdentifiedBaseWork(h.sourceAsString)
          }.toList
          actualWorks should contain theSameElementsAs expectedWorks
        }
    }

  private def populateElasticsearch(indexName: String): List[IdentifiedWork] = {
    val works = createIdentifiedWorks(count = 10)

    insertIntoElasticsearch(indexName, itemType, works: _*)

    works.sortBy(_.canonicalId).toList
  }

  private def createListQueryOptions(
    workType: Option[String] = None,
    indexName: String,
    limit: Int = 10,
    from: Int = 0
  ): ElasticsearchQueryOptions =
    ElasticsearchQueryOptions(
      sortByField = Some("canonicalId"),
      workType = workType,
      indexName = indexName,
      limit = limit,
      from = from
    )

  private def jsonToIdentifiedBaseWork(document: String): IdentifiedBaseWork =
    fromJson[IdentifiedBaseWork](document).get
}
