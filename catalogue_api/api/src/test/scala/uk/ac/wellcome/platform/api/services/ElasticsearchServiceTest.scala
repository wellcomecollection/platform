package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchResponse}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.api.fixtures.ElasticsearchServiceFixture
import uk.ac.wellcome.platform.api.generators.SearchOptionsGenerators
import uk.ac.wellcome.platform.api.models.{
  ItemLocationTypeFilter,
  WorkTypeFilter
}

import scala.concurrent.Future
import scala.util.Random

class ElasticsearchServiceTest
    extends FunSpec
    with ElasticsearchServiceFixture
    with Matchers
    with ScalaFutures
    with SearchOptionsGenerators
    with WorksGenerators {

  val itemType = documentType

  describe("simpleStringQueryResults") {
    it("finds results for a simpleStringQuery search") {
      withLocalElasticsearchIndex { indexName =>
        val work1 = createIdentifiedWorkWith(title = "Amid an Aegean")
        val work2 = createIdentifiedWorkWith(title = "Before a Bengal")
        val work3 = createIdentifiedWorkWith(title = "Circling a Cheetah")

        insertIntoElasticsearch(indexName, itemType, work1, work2, work3)

        assertSearchResultsAreCorrect(
          indexName = indexName,
          queryString = "Aegean",
          expectedWorks = List(work1)
        )
      }
    }

    it("filters search results by workType") {
      withLocalElasticsearchIndex { indexName =>
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
          indexName = indexName,
          queryString = "artichokes",
          queryOptions = createElasticsearchQueryOptionsWith(
            filters = List(WorkTypeFilter(workTypeId = "b"))
          ),
          expectedWorks = List(workWithCorrectWorkType)
        )
      }
    }

    it("filters search results with multiple workTypes") {
      withLocalElasticsearchIndex { indexName =>
        val work1 = createIdentifiedWorkWith(
          title = "Animated artichokes",
          workType = Some(WorkType(id = "b", label = "Books"))
        )
        val workWithWrongTitle = createIdentifiedWorkWith(
          title = "Bouncing bananas",
          workType = Some(WorkType(id = "b", label = "Books"))
        )
        val work2 = createIdentifiedWorkWith(
          title = "Animated artichokes",
          workType = Some(WorkType(id = "m", label = "Manuscripts"))
        )
        val workWithWrongType = createIdentifiedWorkWith(
          title = "Animated artichokes",
          workType = Some(WorkType(id = "a", label = "Archives"))
        )

        insertIntoElasticsearch(
          indexName,
          itemType,
          work1,
          workWithWrongTitle,
          work2,
          workWithWrongType)

        assertSearchResultsAreCorrect(
          indexName = indexName,
          queryString = "artichokes",
          queryOptions = createElasticsearchQueryOptionsWith(
            filters = List(WorkTypeFilter(List("b", "m")))
          ),
          expectedWorks = List(work1, work2)
        )
      }
    }

    it("filters results by item locationType") {
      withLocalElasticsearchIndex { indexName =>
        val work = createIdentifiedWorkWith(
          title = "Tumbling tangerines",
          items = List(
            createItemWithLocationType(LocationType("iiif-image")),
            createItemWithLocationType(LocationType("acqi"))
          )
        )

        val notMatchingWork = createIdentifiedWorkWith(
          title = "Tumbling tangerines",
          items = List(
            createItemWithLocationType(LocationType("acqi"))
          )
        )

        insertIntoElasticsearch(indexName, itemType, work, notMatchingWork)

        assertSearchResultsAreCorrect(
          indexName = indexName,
          queryString = "tangerines",
          queryOptions = createElasticsearchQueryOptionsWith(
            filters =
              List(ItemLocationTypeFilter(locationTypeId = "iiif-image"))
          ),
          expectedWorks = List(work)
        )
      }
    }

    it("filters results by multiple item locationTypes") {
      withLocalElasticsearchIndex { indexName =>
        val work = createIdentifiedWorkWith(
          title = "Tumbling tangerines",
          items = List(
            createItemWithLocationType(LocationType("iiif-image")),
            createItemWithLocationType(LocationType("acqi"))
          )
        )

        val notMatchingWork = createIdentifiedWorkWith(
          title = "Tumbling tangerines",
          items = List(
            createItemWithLocationType(LocationType("acqi"))
          )
        )

        val work2 = createIdentifiedWorkWith(
          title = "Tumbling tangerines",
          items = List(
            createItemWithLocationType(LocationType("digit"))
          )
        )

        insertIntoElasticsearch(
          indexName,
          itemType,
          work,
          notMatchingWork,
          work2)

        assertSearchResultsAreCorrect(
          indexName = indexName,
          queryString = "tangerines",
          queryOptions = createElasticsearchQueryOptionsWith(
            filters = List(
              ItemLocationTypeFilter(
                locationTypeIds = List("iiif-image", "digit")))
          ),
          expectedWorks = List(work, work2)
        )
      }
    }
  }

  describe("findResultById") {
    it("finds a result by ID") {
      withLocalElasticsearchIndex { indexName =>
        val work = createIdentifiedWork

        insertIntoElasticsearch(indexName, itemType, work)

        withElasticsearchService { searchService =>
          val documentOptions =
            createElasticsearchDocumentOptionsWith(indexName)

          val searchResultFuture: Future[GetResponse] =
            searchService.findResultById(canonicalId = work.canonicalId)(
              documentOptions)

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
      withLocalElasticsearchIndex { indexName =>
        val work1 = createIdentifiedWorkWith(canonicalId = "000Z")
        val work2 = createIdentifiedWorkWith(canonicalId = "000Y")
        val work3 = createIdentifiedWorkWith(canonicalId = "000X")

        insertIntoElasticsearch(indexName, itemType, work1, work2, work3)

        assertListResultsAreCorrect(
          indexName = indexName,
          expectedWorks = List(work3, work2, work1)
        )

      // TODO: canonicalID is the only user-defined field that we can sort on.
      // When we have other fields we can sort on, we should extend this test
      // for different sort orders.
      }
    }

    it("returns everything if we ask for a limit > result size") {
      withLocalElasticsearchIndex { indexName =>
        val works = populateElasticsearch(indexName)

        val queryOptions = createElasticsearchQueryOptionsWith(
          limit = works.length + 1
        )

        assertListResultsAreCorrect(
          indexName = indexName,
          queryOptions = queryOptions,
          expectedWorks = works
        )
      }
    }

    it("returns a page from the beginning of the result set") {
      withLocalElasticsearchIndex { indexName =>
        val works = populateElasticsearch(indexName)

        val queryOptions = createElasticsearchQueryOptionsWith(limit = 4)

        assertListResultsAreCorrect(
          indexName = indexName,
          queryOptions = queryOptions,
          expectedWorks = works.slice(0, 4)
        )
      }
    }

    it("returns a page from halfway through the result set") {
      withLocalElasticsearchIndex { indexName =>
        val works = populateElasticsearch(indexName)

        val queryOptions = createElasticsearchQueryOptionsWith(
          limit = 4,
          from = 3
        )

        assertListResultsAreCorrect(
          indexName = indexName,
          queryOptions = queryOptions,
          expectedWorks = works.slice(3, 7)
        )
      }
    }

    it("returns a page from the end of the result set") {
      withLocalElasticsearchIndex { indexName =>
        val works = populateElasticsearch(indexName)

        val queryOptions = createElasticsearchQueryOptionsWith(
          limit = 7,
          from = 5
        )

        assertListResultsAreCorrect(
          indexName = indexName,
          queryOptions = queryOptions,
          expectedWorks = works.slice(5, 10)
        )
      }
    }

    it("returns an empty page if asked for a limit > result size") {
      withLocalElasticsearchIndex { indexName =>
        val works = populateElasticsearch(indexName)

        val queryOptions = createElasticsearchQueryOptionsWith(
          from = works.length * 2
        )

        assertListResultsAreCorrect(
          indexName = indexName,
          queryOptions = queryOptions,
          expectedWorks = List()
        )
      }
    }

    it("does not list works that have visible=false") {
      withLocalElasticsearchIndex { indexName =>
        val visibleWorks = createIdentifiedWorks(count = 8)
        val invisibleWorks = createIdentifiedInvisibleWorks(count = 2)

        val works = visibleWorks ++ invisibleWorks
        insertIntoElasticsearch(indexName, itemType, works: _*)

        assertListResultsAreCorrect(
          indexName = indexName,
          expectedWorks = visibleWorks
        )
      }
    }

    it("filters list results by workType") {
      withLocalElasticsearchIndex { indexName =>
        val work1 = createIdentifiedWorkWith(
          workType = Some(WorkType(id = "b", label = "Books"))
        )
        val work2 = createIdentifiedWorkWith(
          workType = Some(WorkType(id = "b", label = "Books"))
        )
        val workWithWrongWorkType = createIdentifiedWorkWith(
          workType = Some(WorkType(id = "m", label = "Manuscripts"))
        )

        insertIntoElasticsearch(
          indexName,
          itemType,
          work1,
          work2,
          workWithWrongWorkType)

        val queryOptions = createElasticsearchQueryOptionsWith(
          filters = List(WorkTypeFilter(workTypeId = "b"))
        )

        assertListResultsAreCorrect(
          indexName = indexName,
          queryOptions = queryOptions,
          expectedWorks = List(work1, work2)
        )
      }
    }

    it("filters list results with multiple workTypes") {
      withLocalElasticsearchIndex { indexName =>
        val work1 = createIdentifiedWorkWith(
          workType = Some(WorkType(id = "b", label = "Books"))
        )
        val work2 = createIdentifiedWorkWith(
          workType = Some(WorkType(id = "b", label = "Books"))
        )
        val work3 = createIdentifiedWorkWith(
          workType = Some(WorkType(id = "a", label = "Archives"))
        )
        val workWithWrongWorkType = createIdentifiedWorkWith(
          workType = Some(WorkType(id = "m", label = "Manuscripts"))
        )

        insertIntoElasticsearch(
          indexName,
          itemType,
          work1,
          work2,
          work3,
          workWithWrongWorkType)

        val queryOptions = createElasticsearchQueryOptionsWith(
          filters = List(WorkTypeFilter(List("b", "a")))
        )

        assertListResultsAreCorrect(
          indexName = indexName,
          queryOptions = queryOptions,
          expectedWorks = List(work1, work2, work3)
        )
      }
    }
  }

  private def createItemWithLocationType(
    locationType: LocationType): Identified[Item] =
    createIdentifiedItemWith(
      locations = List(
        // This test really shouldn't be affected by physical/digital locations;
        // we just pick randomly here to ensure we get a good mixture.
        Random
          .shuffle(
            List(
              createPhysicalLocationWith(locationType = locationType),
              createDigitalLocationWith(locationType = locationType)
            ))
          .head
      )
    )

  private def populateElasticsearch(indexName: String): List[IdentifiedWork] = {
    val works = createIdentifiedWorks(count = 10)

    insertIntoElasticsearch(indexName, itemType, works: _*)

    works.sortBy(_.canonicalId).toList
  }

  private def assertSearchResultsAreCorrect(
    indexName: String,
    queryString: String,
    queryOptions: ElasticsearchQueryOptions = createElasticsearchQueryOptions,
    expectedWorks: List[IdentifiedWork]
  ): Assertion =
    withElasticsearchService { searchService =>
      val documentOptions = createElasticsearchDocumentOptionsWith(indexName)

      val searchResponseFuture = searchService
        .simpleStringQueryResults(queryString)(documentOptions, queryOptions)

      whenReady(searchResponseFuture) { response =>
        searchResponseToWorks(response) should contain theSameElementsAs expectedWorks
      }
    }

  private def assertListResultsAreCorrect(
    indexName: String,
    queryOptions: ElasticsearchQueryOptions = createElasticsearchQueryOptions,
    expectedWorks: Seq[IdentifiedWork]
  ): Assertion =
    withElasticsearchService { searchService =>
      val documentOptions = createElasticsearchDocumentOptionsWith(indexName)

      val listResponseFuture = searchService
        .listResults(sortByField = "canonicalId")(documentOptions, queryOptions)

      whenReady(listResponseFuture) { response =>
        searchResponseToWorks(response) should contain theSameElementsAs expectedWorks
      }
    }

  private def searchResponseToWorks(
    response: SearchResponse): List[IdentifiedBaseWork] =
    response.hits.hits.map { h: SearchHit =>
      jsonToIdentifiedBaseWork(h.sourceAsString)
    }.toList

  private def jsonToIdentifiedBaseWork(document: String): IdentifiedBaseWork =
    fromJson[IdentifiedBaseWork](document).get
}
