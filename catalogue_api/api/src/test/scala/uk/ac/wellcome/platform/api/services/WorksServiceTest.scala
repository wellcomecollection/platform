package uk.ac.wellcome.platform.api.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.platform.api.WorksUtil
import uk.ac.wellcome.models.WorksIncludes
import uk.ac.wellcome.display.models.{DisplayIdentifier, DisplayWork}
import uk.ac.wellcome.platform.api.models.DisplayResultList
import uk.ac.wellcome.test.utils.IndexedElasticSearchLocal

import scala.concurrent.ExecutionContext.Implicits.global

class WorksServiceTest
    extends FunSpec
    with IndexedElasticSearchLocal
    with Matchers
    with ScalaFutures
    with WorksUtil {

  val indexName = "works"
  val itemType = "work"

  val searchService =
    new ElasticSearchService(indexName, itemType, elasticClient)

  val worksService =
    new WorksService(10, searchService)

  it("should return the records in Elasticsearch") {
    val works = createWorks(2)

    insertIntoElasticSearch(works: _*)

    val displayWorksFuture = worksService.listWorks()

    displayWorksFuture map { displayWork =>
      displayWork.results should have size 2
      displayWork.results.head shouldBe DisplayWork(
        works(0).canonicalId,
        works(0).title.get)
      displayWork.results.tail.head shouldBe DisplayWork(
        works(1).canonicalId,
        works(1).title.get)
    }
  }

  it("should get a DisplayWork by id") {
    val works = createWorks(1)

    insertIntoElasticSearch(works: _*)

    val recordsFuture = worksService.findWorkById(works.head.canonicalId)

    whenReady(recordsFuture) { records =>
      records.isDefined shouldBe true
      records.get shouldBe works.head
    }
  }

  it("should only find results that match a query if doing a full-text search") {
    val workDodo = workWith(
      canonicalId = "1234",
      title = "A drawing of a dodo"
    )
    val workMouse = workWith(
      canonicalId = "5678",
      title = "A mezzotint of a mouse"
    )

    insertIntoElasticSearch(workDodo, workMouse)

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

  it("should return a future of None if it cannot get a record by id") {
    val recordsFuture = worksService.findWorkById("1234")

    whenReady(recordsFuture) { record =>
      record shouldBe None
    }
  }

  it("should return 0 pages when no results are available") {
    val displayWorksFuture = worksService.listWorks(pageSize = 10)

    whenReady(displayWorksFuture) { works =>
      works.totalResults shouldBe 0
    }
  }

  it(
    "should return an empty result set when asked for a page that does not exist") {
    val works = createWorks(3)

    insertIntoElasticSearch(works: _*)

    val displayWorksFuture =
      worksService.listWorks(pageSize = 1, pageNumber = 4)

    whenReady(displayWorksFuture) { receivedWorks =>
      receivedWorks.results shouldBe empty
    }
  }

  it(
    "should not throw an exception if passed an invalid query string for full-text search") {
    val workEmu = workWith(
      canonicalId = "1234",
      title = "An etching of an emu"
    )
    insertIntoElasticSearch(workEmu)

    val searchForEmu = worksService.searchWorks(
      "emu \"unmatched quotes are a lexical error in the Elasticsearch parser"
    )

    whenReady(searchForEmu) { works =>
      works.results should have size 1
      works.results.head shouldBe workEmu
    }
  }
}
