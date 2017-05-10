package uk.ac.wellcome.platform.api.services

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.models.{IdentifiedWork, SourceIdentifier, Work}
import uk.ac.wellcome.platform.api.models.DisplayWork
import uk.ac.wellcome.test.utils.IndexedElasticSearchLocal
import scala.concurrent.ExecutionContext.Implicits.global

class ElasticSearchServiceTest
    extends FunSpec
    with IndexedElasticSearchLocal
    with Matchers
    with ScalaFutures {

  val elasticService =
    new ElasticSearchService(indexName, itemType, elasticClient)

  it("should return the records in Elasticsearch") {
    val firstIdentifiedWork =
      identifiedWorkWith(canonicalId = "1234",
                         label = "this is the first item label")
    val secondIdentifiedWork =
      identifiedWorkWith(canonicalId = "5678",
                         label = "this is the second item label")
    insertIntoElasticSearch(firstIdentifiedWork, secondIdentifiedWork)

    val displayWorksFuture = elasticService.findWork()

    displayWorksFuture map { displayWork =>
      displayWork should have size 2
      displayWork.head shouldBe DisplayWork("Work",
                                            firstIdentifiedWork.canonicalId,
                                            firstIdentifiedWork.work.label)
      displayWork.tail.head shouldBe DisplayWork(
        "Work",
        secondIdentifiedWork.canonicalId,
        secondIdentifiedWork.work.label)
    }
  }

  it("should get a DisplayWork by id") {
    insertIntoElasticSearch(
      identifiedWorkWith(canonicalId = "1234",
                         label = "this is the item label"))

    val recordsFuture = elasticService.findWorkById("1234")

    whenReady(recordsFuture) { records =>
      records.isDefined shouldBe true
      records.get shouldBe DisplayWork("Work",
                                       "1234",
                                       "this is the item label",
                                       None)
    }
  }

  it("should only find results that match a query if doing a full-text search") {
    val workDodo = identifiedWorkWith(
      canonicalId = "1234",
      label = "A drawing of a dodo"
    )
    val workMouse = identifiedWorkWith(
      canonicalId = "5678",
      label = "A mezzotint of a mouse"
    )
    insertIntoElasticSearch(workDodo, workMouse)

    val searchForCat = elasticService.fullTextSearchWorks("cat")
    whenReady(searchForCat) { works =>
      works should have size 0
    }

    val searchForDodo = elasticService.fullTextSearchWorks("dodo")
    whenReady(searchForDodo) { works =>
      works should have size 1
      works.head shouldBe DisplayWork("Work",
                                      workDodo.canonicalId,
                                      workDodo.work.label)
    }
  }

  it("should return a future of None if it cannot get arecord by id") {
    val recordsFuture = elasticService.findWorkById("1234")

    whenReady(recordsFuture) { record =>
      record shouldBe None
    }
  }

  it("should not throw an exception if passed an invalid query string for full-text search") {
    val workEmu = identifiedWorkWith(
      canonicalId = "1234",
      label = "An etching of an emu"
    )
    insertIntoElasticSearch(workEmu)

    val searchForEmu = elasticService.fullTextSearchWorks(
      "emu \"unmatched quotes are a lexical error in the Elasticsearch parser"
    )

    whenReady(searchForEmu) { works =>
      works should have size 1
      works.head shouldBe DisplayWork("Work",
                                      workEmu.canonicalId,
                                      workEmu.work.label)
    }
  }

  private def identifiedWorkWith(canonicalId: String, label: String) = {
    IdentifiedWork(canonicalId,
                   Work(identifiers = List(
                          SourceIdentifier(source = "Calm",
                                           sourceId = "AltRefNo",
                                           value = "calmid")),
                        label = label))
  }
}
