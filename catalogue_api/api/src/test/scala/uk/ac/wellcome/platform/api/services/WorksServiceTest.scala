package uk.ac.wellcome.platform.api.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.test.util.WorksGenerators
import uk.ac.wellcome.platform.api.fixtures.{
  ElasticsearchServiceFixture,
  WorksServiceFixture
}

class WorksServiceTest
    extends FunSpec
    with ElasticsearchServiceFixture
    with WorksServiceFixture
    with Matchers
    with ScalaFutures
    with WorksGenerators {

  val itemType = "work"

  describe("listWorks") {
    it("gets records in Elasticsearch") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        withElasticsearchService(indexName = indexName, itemType = itemType) {
          searchService =>
            withWorksService(searchService) { worksService =>
              val works = createIdentifiedWorks(count = 2)

              insertIntoElasticsearch(indexName, itemType, works: _*)

              val future = worksService.listWorks(indexName = indexName)

              whenReady(future) { resultList =>
                resultList.results should contain theSameElementsAs works
              }
            }
        }
      }
    }

    it("returns 0 pages when no results are available") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        withElasticsearchService(indexName = indexName, itemType = itemType) {
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
        withElasticsearchService(indexName = indexName, itemType = itemType) {
          searchService =>
            withWorksService(searchService) { worksService =>
              val works = createIdentifiedWorks(count = 3)

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
  }

  describe("findWorkById") {
    it("gets a DisplayWork by id") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        withElasticsearchService(indexName = indexName, itemType = itemType) {
          searchService =>
            withWorksService(searchService) { worksService =>
              val work = createIdentifiedWork

              insertIntoElasticsearch(indexName, itemType, work)

              val recordsFuture =
                worksService.findWorkById(
                  canonicalId = work.canonicalId,
                  indexName = indexName
                )

              whenReady(recordsFuture) { records =>
                records.isDefined shouldBe true
                records.get shouldBe work
              }
            }
        }
      }
    }

    it("returns a future of None if it cannot get a record by id") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        withElasticsearchService(indexName = indexName, itemType = itemType) {
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
  }

  describe("searchWorks") {
    it("only finds results that match a query if doing a full-text search") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        withElasticsearchService(indexName = indexName, itemType = itemType) {
          searchService =>
            withWorksService(searchService) { worksService =>
              val workDodo = createIdentifiedWorkWith(
                title = "A drawing of a dodo"
              )
              val workMouse = createIdentifiedWorkWith(
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

    it(
      "doesn't throw an exception if passed an invalid query string for full-text search") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        withElasticsearchService(indexName = indexName, itemType = itemType) {
          searchService =>
            withWorksService(searchService) { worksService =>
              val workEmu = createIdentifiedWorkWith(
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
}
