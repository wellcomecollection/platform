package uk.ac.wellcome.platform.api.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.WorkType
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
        withElasticsearchService { searchService =>
          withWorksService(searchService) { worksService =>
            val works = createIdentifiedWorks(count = 2)

            insertIntoElasticsearch(indexName, itemType, works: _*)

            val future = worksService.listWorks(
              createWorksSearchOptionsWith(indexName = indexName)
            )

            whenReady(future) { resultList =>
              resultList.results should contain theSameElementsAs works
            }
          }
        }
      }
    }

    it("returns 0 pages when no results are available") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        withElasticsearchService {  searchService =>
          withWorksService(searchService) { worksService =>
            val future = worksService.listWorks(
              createWorksSearchOptionsWith(indexName = indexName))

            whenReady(future) { works =>
              works.totalResults shouldBe 0
            }
          }
        }
      }
    }

    it("returns an empty result set when asked for a page that does not exist") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        withElasticsearchService { searchService =>
          withWorksService(searchService) { worksService =>
            val works = createIdentifiedWorks(count = 3)

            insertIntoElasticsearch(indexName, itemType, works: _*)

            val future = worksService.listWorks(
              createWorksSearchOptionsWith(
                indexName = indexName,
                pageNumber = 4
              )
            )

            whenReady(future) { receivedWorks =>
              receivedWorks.results shouldBe empty
            }
          }
        }
      }
    }

    it("filters records by workType") {
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

      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        withElasticsearchService { searchService =>
          withWorksService(searchService) { worksService =>
            insertIntoElasticsearch(
              indexName,
              itemType,
              work1,
              work2,
              workWithWrongWorkType)

            val future = worksService.listWorks(
              createWorksSearchOptionsWith(
                workTypeFilter = Some("b"),
                indexName = indexName
              )
            )

            whenReady(future) { resultList =>
              resultList.results should contain theSameElementsAs List(
                work1,
                work2)
            }
          }
        }
      }
    }
  }

  describe("findWorkById") {
    it("gets a DisplayWork by id") {
      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        withElasticsearchService { searchService =>
          withWorksService(searchService) { worksService =>
            val work = createIdentifiedWork

            insertIntoElasticsearch(indexName, itemType, work)

            val recordsFuture =
              worksService.findWorkById(
                canonicalId = work.canonicalId,
                indexName = indexName,
                documentType = itemType
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
        withElasticsearchService { searchService =>
          withWorksService(searchService) { worksService =>
            val recordsFuture = worksService.findWorkById(
              canonicalId = "1234",
              indexName = indexName,
              documentType = itemType
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
        withElasticsearchService { searchService =>
          withWorksService(searchService) { worksService =>
            val workDodo = createIdentifiedWorkWith(
              title = "A drawing of a dodo"
            )
            val workMouse = createIdentifiedWorkWith(
              title = "A mezzotint of a mouse"
            )

            insertIntoElasticsearch(indexName, itemType, workDodo, workMouse)

            val searchForCat = worksService.searchWorks(query = "cat")(
              createWorksSearchOptionsWith(indexName = indexName)
            )

            whenReady(searchForCat) { works =>
              works.results should have size 0
            }

            val searchForDodo = worksService.searchWorks(query = "dodo")(
              createWorksSearchOptionsWith(indexName = indexName)
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
        withElasticsearchService { searchService =>
          withWorksService(searchService) { worksService =>
            val workEmu = createIdentifiedWorkWith(
              title = "An etching of an emu"
            )
            insertIntoElasticsearch(indexName, itemType, workEmu)

            val searchForEmu = worksService.searchWorks(query =
              "emu \"unmatched quotes are a lexical error in the Elasticsearch parser")(
              createWorksSearchOptionsWith(indexName = indexName)
            )

            whenReady(searchForEmu) { works =>
              works.results should have size 1
              works.results.head shouldBe workEmu
            }
          }
        }
      }
    }

    it("filters searches by workType") {
      val matchingWork = createIdentifiedWorkWith(
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

      withLocalElasticsearchIndex(itemType = itemType) { indexName =>
        withElasticsearchService { searchService =>
          withWorksService(searchService) { worksService =>
            insertIntoElasticsearch(
              indexName,
              itemType,
              matchingWork,
              workWithWrongTitle,
              workWithWrongWorkType)

            val searchForEmu = worksService.searchWorks(
              query = "artichokes")(
              createWorksSearchOptionsWith(
                workTypeFilter = Some("b"),
                indexName = indexName
              )
            )

            whenReady(searchForEmu) { works =>
              works.results shouldBe List(matchingWork)
            }
          }
        }
      }
    }
  }

  private def createWorksSearchOptionsWith(
    workTypeFilter: Option[String] = None,
    indexName: String,
    pageSize: Int = 10,
    pageNumber: Int = 1
  ): WorksSearchOptions =
    WorksSearchOptions(
      workTypeFilter = workTypeFilter,
      documentType = itemType,
      indexName = indexName,
      pageSize = pageSize,
      pageNumber = pageNumber
    )
}
