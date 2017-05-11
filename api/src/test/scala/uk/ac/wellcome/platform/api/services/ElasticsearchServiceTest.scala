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

    val sortedSearchResultByCanonicalId = searchService.findResults(
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



  it("should return the correct number of results from Elasticsearch") {
    val work1 = identifiedWorkWith(
      canonicalId = "0001",
      label = "The first flounder"
    )
    val work2 = identifiedWorkWith(
      canonicalId = "0002",
      label = "The second salmon"
    )
    val work3 = identifiedWorkWith(
      canonicalId = "0003",
      label = "The third trout"
    )
    val work4 = identifiedWorkWith(
      canonicalId = "0004",
      label = "The fourth flagtail"
    )
    val work5 = identifiedWorkWith(
      canonicalId = "0005",
      label = "The fifth flagfin"
    )

    insertIntoElasticSearch(work1, work2, work3, work4, work5)

    val searchResultFuture = searchService.findResults(
      sortByField = "canonicalId",
      limit = 4
    )
    whenReady(searchResultFuture) { _.hits should have size 4 }

    val searchResultFutureWithLargeLimit = searchService.findResults(
      sortByField = "canonicalId",
      limit = 10
    )
    whenReady(searchResultFutureWithLargeLimit) { _.hits should have size 5 }
  }

  it("should be able to fetch from midway through the Elasticsearch results") {
    val work1 = identifiedWorkWith(
      canonicalId = "0001",
      label = "Ascending the Alps"
    )
    val work2 = identifiedWorkWith(
      canonicalId = "0002",
      label = "Braving the Black Hills"
    )
    val work3 = identifiedWorkWith(
      canonicalId = "0003",
      label = "Climbing the Cairngorms"
    )
    val work4 = identifiedWorkWith(
      canonicalId = "0004",
      label = "Daring on Drakensberg"
    )

    insertIntoElasticSearch(work1, work2, work3, work4)

    val sortedSearchResultByCanonicalId = searchService.findResults(
      sortByField = "canonicalId",
      from = 2
    )
    whenReady(sortedSearchResultByCanonicalId) { result =>
      result.hits should have size 2
      val works = result.hits.map { DisplayWork(_) }
      works.head shouldBe DisplayWork("Work", work3.canonicalId, work3.work.label)
      works.last shouldBe DisplayWork("Work", work4.canonicalId, work4.work.label)
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
