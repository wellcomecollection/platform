package uk.ac.wellcome.platform.api.services

import org.scalatest.compatible.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.platform.api.WorksUtil
import uk.ac.wellcome.models.WorksIncludes
import uk.ac.wellcome.display.models.{DisplayIdentifier, DisplayWork}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.platform.api.models.DisplayResultList
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

class WorksServiceTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ElasticsearchFixtures
    with WorksUtil {

  val itemType = "work"

  private def withWorksService(index: String)(
    testWith: TestWith[WorksService, Assertion]) = {

    val searchService = new ElasticSearchService(
      defaultIndex = index,
      documentType = itemType,
      elasticClient = elasticClient
    )
    val worksService = new WorksService(
      defaultPageSize = 10,
      searchService = searchService
    )

    testWith(worksService)
  }

  it("returns the records in Elasticsearch") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withWorksService(indexName) { worksService =>
        val works = createWorks(2)

        insertIntoElasticsearch(indexName = indexName, itemType = itemType, works: _*)

        val resultListFuture = worksService.listWorks()

        whenReady(resultListFuture) { resultList =>
          resultList.results should have size 2
          resultList.results.head shouldBe works(0)
          resultList.results.tail.head shouldBe works(1)
        }
      }
    }
  }

  it("gets a DisplayWork by id") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withWorksService(indexName) { worksService =>
        val works = createWorks(1)

        insertIntoElasticsearch(indexName = indexName, itemType = itemType, works: _*)

        val recordsFuture = worksService.findWorkById(works.head.canonicalId)

        whenReady(recordsFuture) { records =>
          records.isDefined shouldBe true
          records.get shouldBe works.head
        }
      }
    }
  }

  it("finds results that match a query if doing a full-text search") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withWorksService(indexName) { worksService =>
        val workDodo = workWith(
          canonicalId = "1234",
          title = "A drawing of a dodo"
        )
        val workMouse = workWith(
          canonicalId = "5678",
          title = "A mezzotint of a mouse"
        )

        val works = List(workDodo, workMouse)
        insertIntoElasticsearch(indexName = indexName, itemType = itemType, works: _*)

        val searchForCat = worksService.searchWorks("cat")

        whenReady(searchForCat) { works =>
          works.results should have size 0
        }

        val searchForDodo = worksService.searchWorks("dodo")
        whenReady(searchForDodo) { works =>
          works.results should have size 1
          works.results.head shouldBe workDodo
        }
      }
    }
  }

  it("returns a future of None if it cannot get a record by id") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withWorksService(indexName) { worksService =>
        val recordsFuture = worksService.findWorkById("1234")

        whenReady(recordsFuture) { record =>
          record shouldBe None
        }
      }
    }
  }

  it("returns 0 pages when no results are available") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withWorksService(indexName) { worksService =>
        val displayWorksFuture = worksService.listWorks(pageSize = 10)

        whenReady(displayWorksFuture) { works =>
          works.totalResults shouldBe 0
        }
      }
    }
  }

  it("returns an empty result set when asked for a page that does not exist") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withWorksService(indexName) { worksService =>
        val works = createWorks(3)
        insertIntoElasticsearch(indexName = indexName, itemType = itemType, works: _*)

        val displayWorksFuture =
          worksService.listWorks(pageSize = 1, pageNumber = 4)

        whenReady(displayWorksFuture) { receivedWorks =>
          receivedWorks.results shouldBe empty
        }
      }
    }
  }

  it(
    "doesn't throw an exception if passed an invalid query string for full-text search") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withWorksService(indexName) { worksService =>
        val workEmu = workWith(
          canonicalId = "1234",
          title = "An etching of an emu"
        )
        insertIntoElasticsearch(indexName = indexName, itemType = itemType, workEmu)

        val searchForEmu = worksService.searchWorks(
          "emu \"unmatched quotes are a lexical error in the Elasticsearch parser"
        )

        whenReady(searchForEmu) { works =>
          works.results should have size 1
          works.results.head shouldBe workEmu
        }
      }
    }
  }
}
