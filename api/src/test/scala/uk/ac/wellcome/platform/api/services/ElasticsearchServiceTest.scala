package uk.ac.wellcome.platform.api.services

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.models.{IdentifiedWork, SourceIdentifier, Work}
import uk.ac.wellcome.platform.api.models.DisplayWork
import uk.ac.wellcome.test.utils.IndexedElasticSearchLocal
import scala.concurrent.ExecutionContext.Implicits.global

class ElasticsearchServiceTest
    extends FunSpec
    with IndexedElasticSearchLocal
    with Matchers
    with ScalaFutures {

  val searchService =
    new ElasticSearchService(indexName, itemType, elasticClient)

  it("should sort results from Elasticsearch in the correct order") {
    val work1 = identifiedWorkWith(
      canonicalId = "000Z",
      label = "Amid an Aegean"
    )
    val work2 = identifiedWorkWith(
      canonicalId = "000Y",
      label = "Before a Bengal"
    )
    val work3 = identifiedWorkWith(
      canonicalId = "000X",
      label = "Circling a Cheetah"
    )

    insertIntoElasticSearch(work1, work2, work3)

    val sortedSearchResultByCanonicalId = searchService.listResults(
      sortByField = "canonicalId"
    )
    whenReady(sortedSearchResultByCanonicalId) { result =>
      val works = result.hits.map { DisplayWork(_) }
      works.head shouldBe DisplayWork("Work", work3.canonicalId, work3.work.label)
      works.last shouldBe DisplayWork("Work", work1.canonicalId, work1.work.label)
    }

    // TODO: canonicalID is the only user-defined field that we can sort on.
    // When we have other fields we can sort on, we should extend this test
    // for different sort orders.
  }

  /** Populate the test index with some works, and return the corresponding
   *  `DisplayWork` instances.
   */
  private def populateElasticsearch(count: Int = 10): List[DisplayWork] = {
    val works: List[IdentifiedWork] = (0 to 9).map { x =>
      identifiedWorkWith(canonicalId = s"ID-$x", label=s"Work number $x")
    }.toList
    insertIntoElasticSearch(works: _*)

    works.map { work: IdentifiedWork =>
      DisplayWork("Work", work.canonicalId, work.work.label)
    }
  }

  it("should return everything if we ask for a limit > result size") {
    val displayWorks = populateElasticsearch()
    assertSliceIsCorrect(
      limit = displayWorks.length + 1,
      from = 0,
      expectedWorks = displayWorks
    )
  }

  it("should return a page from the beginning of the result set") {
    val displayWorks = populateElasticsearch()
    assertSliceIsCorrect(
      limit = 4,
      from = 0,
      expectedWorks = displayWorks.slice(0, 4)
    )
  }

  it("should return a page from halfway through the result set") {
    val displayWorks = populateElasticsearch()
    assertSliceIsCorrect(
      limit = 4,
      from = 3,
      expectedWorks = displayWorks.slice(3, 7)
    )
  }

  it("should return a page from the end of the result set") {
    val displayWorks = populateElasticsearch()
    assertSliceIsCorrect(
      limit = 7,
      from = 5,
      expectedWorks = displayWorks.slice(5, 10)
    )
  }

  it("should return an empty page if asked for a limit > result size") {
    val displayWorks = populateElasticsearch()
    assertSliceIsCorrect(
      limit = 10,
      from = displayWorks.length * 2,
      expectedWorks = List()
    )
  }

  private def assertSliceIsCorrect(
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
      val returnedWorks = result.hits.map { DisplayWork(_) }
      returnedWorks shouldBe expectedWorks
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
