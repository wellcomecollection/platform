package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.http.search.SearchHit
import org.scalatest.compatible.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.IdentifiedWork
import uk.ac.wellcome.platform.api.WorksUtil
import uk.ac.wellcome.models.WorksIncludes
import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.utils.JsonUtil._

class ElasticsearchServiceTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ElasticsearchFixtures
    with WorksUtil {

  val itemType = "work"

  private def withElasticSearchService(indexName: String)(
    testWith: TestWith[ElasticSearchService, Assertion]) = {

    val searchService = new ElasticSearchService(
      defaultIndex = indexName,
      documentType = itemType,
      elasticClient = elasticClient
    )

    testWith(searchService)
  }

  it("should sort results from Elasticsearch in the correct order") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withElasticSearchService(indexName) { searchService =>
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

        val works = List(work3, work2, work1)
        insertIntoElasticsearch(
          indexName = indexName,
          itemType = itemType,
          works: _*)

        assertSliceIsCorrect(
          searchService = searchService,
          limit = 3,
          from = 0,
          expectedWorks = works.map { DisplayWork(_) }
        )

      // TODO: canonicalID is the only user-defined field that we can sort on.
      // When we have other fields we can sort on, we should extend this test
      // for different sort orders.
      }
    }
  }

  it("should return everything if we ask for a limit > result size") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withElasticSearchService(indexName) { searchService =>
        val displayWorks = populateElasticsearch(indexName = indexName)

        assertSliceIsCorrect(
          searchService = searchService,
          limit = displayWorks.length + 1,
          from = 0,
          expectedWorks = displayWorks
        )
      }
    }
  }

  it("should return a page from the beginning of the result set") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withElasticSearchService(indexName) { searchService =>
        val displayWorks = populateElasticsearch(indexName = indexName)

        assertSliceIsCorrect(
          searchService = searchService,
          limit = 4,
          from = 0,
          expectedWorks = displayWorks.slice(0, 4)
        )
      }
    }
  }

  it("should return a page from halfway through the result set") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withElasticSearchService(indexName) { searchService =>
        val displayWorks = populateElasticsearch(indexName = indexName)

        assertSliceIsCorrect(
          searchService = searchService,
          limit = 4,
          from = 3,
          expectedWorks = displayWorks.slice(3, 7)
        )
      }
    }
  }

  it("should return a page from the end of the result set") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withElasticSearchService(indexName) { searchService =>
        val displayWorks = populateElasticsearch(indexName = indexName)

        assertSliceIsCorrect(
          searchService = searchService,
          limit = 7,
          from = 5,
          expectedWorks = displayWorks.slice(5, 10)
        )
      }
    }
  }

  it("should return an empty page if asked for a limit > result size") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withElasticSearchService(indexName) { searchService =>
        val displayWorks = populateElasticsearch(indexName = indexName)

        assertSliceIsCorrect(
          searchService = searchService,
          limit = 10,
          from = displayWorks.length * 2,
          expectedWorks = List()
        )
      }
    }
  }

  it("should not list works that have visible=false") {
    val visibleWorks = createWorks(count = 8, visible = true)
    val invisibleWorks = createWorks(count = 2, start = 9, visible = false)

    val works = visibleWorks ++ invisibleWorks

    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withElasticSearchService(indexName) { searchService =>
        insertIntoElasticsearch(
          indexName = indexName,
          itemType = itemType,
          works: _*)

        val displayWorks = toDisplayWorks(visibleWorks)

        assertSliceIsCorrect(
          searchService = searchService,
          limit = 10,
          from = 0,
          expectedWorks = displayWorks
        )
      }
    }
  }

  private def populateElasticsearch(indexName: String,
                                    worksIncludes: WorksIncludes =
                                      WorksIncludes()): List[DisplayWork] = {
    val works = createWorks(10)
    insertIntoElasticsearch(
      indexName = indexName,
      itemType = itemType,
      works: _*)

    toDisplayWorks(works, worksIncludes = worksIncludes)
  }

  private def toDisplayWorks(
    works: Seq[IdentifiedWork],
    worksIncludes: WorksIncludes = WorksIncludes()): List[DisplayWork] =
    works.map(DisplayWork(_, worksIncludes)).sortBy(_.id).toList

  private def assertSliceIsCorrect(
    searchService: ElasticSearchService,
    limit: Int,
    from: Int,
    expectedWorks: List[DisplayWork]
  ) = {
    val searchResultFuture = searchService.listResults(
      sortByField = "canonicalId",
      limit = limit,
      from = from
    )
    whenReady(searchResultFuture) { result =>
      result.hits should have size expectedWorks.length
      val returnedWorks = result.hits.hits
        .map { h: SearchHit =>
          jsonToIdentifiedWork(h.sourceAsString)
        }
        .map { DisplayWork(_) }
      returnedWorks.toList shouldBe expectedWorks
    }
  }

  private def jsonToIdentifiedWork(document: String): IdentifiedWork =
    fromJson[IdentifiedWork](document).get
}
