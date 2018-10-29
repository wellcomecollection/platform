package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchResponse}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.{
  IdentifiedBaseWork,
  IdentifiedWork,
  WorkType
}
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

        assertSearchResultsAreCorrect(
          queryString = "Aegean",
          queryOptions =
            createElasticsearchQueryOptionsWith(indexName = indexName),
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

        assertSearchResultsAreCorrect(
          queryString = "artichokes",
          queryOptions = createElasticsearchQueryOptionsWith(
            workTypeFilter = Some("b"),
            indexName = indexName
          ),
          expectedWorks = List(workWithCorrectWorkType)
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

  describe("listResults") {
    it("sorts results from Elasticsearch in the correct order") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val work1 = createIdentifiedWorkWith(canonicalId = "000Z")
        val work2 = createIdentifiedWorkWith(canonicalId = "000Y")
        val work3 = createIdentifiedWorkWith(canonicalId = "000X")

        insertIntoElasticsearch(indexName, itemType, work1, work2, work3)

        val queryOptions =
          createElasticsearchQueryOptionsWith(indexName = indexName)

        assertListResultsAreCorrect(
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
        val works = populateElasticsearch(indexName)

        val queryOptions = createElasticsearchQueryOptionsWith(
          indexName = indexName,
          limit = works.length + 1
        )

        assertListResultsAreCorrect(
          queryOptions = queryOptions,
          expectedWorks = works
        )
      }
    }

    it("returns a page from the beginning of the result set") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val works = populateElasticsearch(indexName)

        val queryOptions = createElasticsearchQueryOptionsWith(
          indexName = indexName,
          limit = 4
        )

        assertListResultsAreCorrect(
          queryOptions = queryOptions,
          expectedWorks = works.slice(0, 4)
        )
      }
    }

    it("returns a page from halfway through the result set") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val works = populateElasticsearch(indexName)

        val queryOptions = createElasticsearchQueryOptionsWith(
          indexName = indexName,
          limit = 4,
          from = 3
        )

        assertListResultsAreCorrect(
          queryOptions = queryOptions,
          expectedWorks = works.slice(3, 7)
        )
      }
    }

    it("returns a page from the end of the result set") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val works = populateElasticsearch(indexName)

        val queryOptions = createElasticsearchQueryOptionsWith(
          indexName = indexName,
          limit = 7,
          from = 5
        )

        assertListResultsAreCorrect(
          queryOptions = queryOptions,
          expectedWorks = works.slice(5, 10)
        )
      }
    }

    it("returns an empty page if asked for a limit > result size") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        val works = populateElasticsearch(indexName)

        val queryOptions = createElasticsearchQueryOptionsWith(
          indexName = indexName,
          from = works.length * 2
        )

        assertListResultsAreCorrect(
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

        val queryOptions =
          createElasticsearchQueryOptionsWith(indexName = indexName)

        assertListResultsAreCorrect(
          queryOptions = queryOptions,
          expectedWorks = visibleWorks
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

        val queryOptions = createElasticsearchQueryOptionsWith(
          workTypeFilter = Some("b"),
          indexName = indexName
        )

        assertListResultsAreCorrect(
          queryOptions = queryOptions,
          expectedWorks = List(work1, work2)
        )
      }
    }
  }

  private def populateElasticsearch(indexName: String): List[IdentifiedWork] = {
    val works = createIdentifiedWorks(count = 10)

    insertIntoElasticsearch(indexName, itemType, works: _*)

    works.sortBy(_.canonicalId).toList
  }

  private def assertSearchResultsAreCorrect(
    queryString: String,
    queryOptions: ElasticsearchQueryOptions,
    expectedWorks: List[IdentifiedWork]
  ): Assertion =
    withElasticsearchService(
      indexName = queryOptions.indexName,
      itemType = itemType) { searchService =>
      val searchResponseFuture = searchService
        .simpleStringQueryResults(queryString)(queryOptions)

      whenReady(searchResponseFuture) { response =>
        searchResponseToWorks(response) should contain theSameElementsAs expectedWorks
      }
    }

  private def assertListResultsAreCorrect(
    queryOptions: ElasticsearchQueryOptions,
    expectedWorks: Seq[IdentifiedWork]
  ): Assertion =
    withElasticsearchService(
      indexName = queryOptions.indexName,
      itemType = itemType) { searchService =>
      val listResponseFuture = searchService
        .listResults(sortByField = "canonicalId")(queryOptions)

      whenReady(listResponseFuture) { response =>
        searchResponseToWorks(response) should contain theSameElementsAs expectedWorks
      }
    }

  private def createElasticsearchQueryOptionsWith(
    workTypeFilter: Option[String] = None,
    indexName: String,
    limit: Int = 10,
    from: Int = 0
  ): ElasticsearchQueryOptions =
    ElasticsearchQueryOptions(
      workTypeFilter = workTypeFilter,
      indexName = indexName,
      limit = limit,
      from = from
    )

  private def searchResponseToWorks(
    response: SearchResponse): List[IdentifiedBaseWork] =
    response.hits.hits.map { h: SearchHit =>
      jsonToIdentifiedBaseWork(h.sourceAsString)
    }.toList

  private def jsonToIdentifiedBaseWork(document: String): IdentifiedBaseWork =
    fromJson[IdentifiedBaseWork](document).get
}
