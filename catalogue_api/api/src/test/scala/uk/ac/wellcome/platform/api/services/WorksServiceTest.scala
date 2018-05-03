package uk.ac.wellcome.platform.api.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.test.util.WorksUtil
import uk.ac.wellcome.platform.api.fixtures.{ElasticsearchServiceFixture, WorksServiceFixture}

class WorksServiceTest
    extends FunSpec
    with ElasticsearchServiceFixture
    with WorksServiceFixture
    with Matchers
    with ScalaFutures
    with WorksUtil {

  val itemType = "work"

  it("gets records in Elasticsearch") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withElasticSearchService(indexName = indexName, itemType = itemType) {
        searchService =>
          withWorksService(searchService) { worksService =>
            val works = createWorks(2)

            insertIntoElasticsearch(indexName, itemType, works: _*)

            val future = worksService.listWorks(indexName = indexName)

            whenReady(future) { resultList =>
              resultList.results shouldBe works
            }
          }
      }
    }
  }

  it("gets a DisplayWork by id") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withElasticSearchService(indexName = indexName, itemType = itemType) {
        searchService =>
          withWorksService(searchService) { worksService =>
            val works = createWorks(1)

            insertIntoElasticsearch(indexName, itemType, works: _*)

            val recordsFuture =
              worksService.findWorkById(
                canonicalId = works.head.canonicalId,
                indexName = indexName
              )

            whenReady(recordsFuture) { records =>
              records.isDefined shouldBe true
              records.get shouldBe works.head
            }
          }
      }
    }
  }

  it("only finds results that match a query if doing a full-text search") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withElasticSearchService(indexName = indexName, itemType = itemType) {
        searchService =>
          withWorksService(searchService) { worksService =>
            val workDodo = workWith(
              canonicalId = "1234",
              title = "A drawing of a dodo"
            )
            val workMouse = workWith(
              canonicalId = "5678",
              title = "A mezzotint of a mouse"
            )

            insertIntoElasticsearch(indexName, itemType, workDodo, workMouse)

            val searchForCat = worksService.searchWorks(
              query = "cat",
              indexName = indexName
            )

            whenReady(searchForCat) { works =>
              works.results should have size 0
            }

            val searchForDodo = worksService.searchWorks(
              query = "dodo",
              indexName = indexName
            )

            whenReady(searchForDodo) { works =>
              works.results should have size 1
              works.results.head shouldBe workDodo
            }
          }
      }
    }
  }

  it("returns a future of None if it cannot get a record by id") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withElasticSearchService(indexName = indexName, itemType = itemType) {
        searchService =>
          withWorksService(searchService) { worksService =>
            val recordsFuture = worksService.findWorkById(
              canonicalId = "1234",
              indexName = indexName
            )

            whenReady(recordsFuture) { record =>
              record shouldBe None
            }
          }
      }
    }
  }

  it("returns 0 pages when no results are available") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withElasticSearchService(indexName = indexName, itemType = itemType) {
        searchService =>
          withWorksService(searchService) { worksService =>
            val displayWorksFuture =
              worksService.listWorks(indexName = indexName, pageSize = 10)

            whenReady(displayWorksFuture) { works =>
              works.totalResults shouldBe 0
            }
          }
      }
    }
  }

  it("returns an empty result set when asked for a page that does not exist") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withElasticSearchService(indexName = indexName, itemType = itemType) {
        searchService =>
          withWorksService(searchService) { worksService =>
            val works = createWorks(3)

            insertIntoElasticsearch(indexName, itemType, works: _*)

            val displayWorksFuture =
              worksService.listWorks(
                indexName = indexName,
                pageSize = 1,
                pageNumber = 4)

            whenReady(displayWorksFuture) { receivedWorks =>
              receivedWorks.results shouldBe empty
            }
          }
      }
    }
  }

  it(
    "throws an exception if passed an invalid query string for full-text search") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      withElasticSearchService(indexName = indexName, itemType = itemType) {
        searchService =>
          withWorksService(searchService) { worksService =>
            val workEmu = workWith(
              canonicalId = "1234",
              title = "An etching of an emu"
            )
            insertIntoElasticsearch(indexName, itemType, workEmu)

            val searchForEmu = worksService.searchWorks(
              query =
                "emu \"unmatched quotes are a lexical error in the Elasticsearch parser",
              indexName = indexName
            )

            whenReady(searchForEmu) { works =>
              works.results should have size 1
              works.results.head shouldBe workEmu
            }
          }
      }
    }
  }
}
