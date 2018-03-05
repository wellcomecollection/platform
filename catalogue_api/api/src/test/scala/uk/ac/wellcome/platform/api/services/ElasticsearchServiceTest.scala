package uk.ac.wellcome.platform.api.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.IdentifiedWork
import uk.ac.wellcome.platform.api.WorksUtil
import uk.ac.wellcome.platform.api.models.{DisplayWork, WorksIncludes}
import uk.ac.wellcome.test.utils.IndexedElasticSearchLocal
import uk.ac.wellcome.utils.JsonUtil._

class ElasticsearchServiceTest
    extends FunSpec
    with IndexedElasticSearchLocal
    with Matchers
    with ScalaFutures
    with WorksUtil {

  val indexName = "works"
  val itemType = "work"

  val searchService =
    new ElasticSearchService(indexName, itemType, elasticClient)

  it("should sort results from Elasticsearch in the correct order") {
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

    assertSliceIsCorrect(
      limit = 3,
      from = 0,
      expectedWorks = List(
        work3,
        work2,
        work1
      )
    )

    // TODO: canonicalID is the only user-defined field that we can sort on.
    // When we have other fields we can sort on, we should extend this test
    // for different sort orders.
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

  it("should not list works that have visible=false") {
    val visibleWorks = createWorks(count = 8, visible = true)
    val invisibleWorks = createWorks(count = 2, start = 9, visible = false)

    val works = visibleWorks ++ invisibleWorks
    insertIntoElasticSearch(works: _*)

    val displayWorks = toDisplayWorks(visibleWorks)

    assertSliceIsCorrect(
      limit = 10,
      from = 0,
      expectedWorks = displayWorks
    )
  }

  private def populateElasticsearch(
    worksIncludes: WorksIncludes = WorksIncludes()): List[DisplayWork] = {
    val works = createWorks(10)
    insertIntoElasticSearch(works: _*)

    toDisplayWorks(works, worksIncludes = worksIncludes)
  }

  private def toDisplayWorks(
    works: Seq[IdentifiedWork],
    worksIncludes: WorksIncludes = WorksIncludes()): List[DisplayWork] =
    works.map(DisplayWork(_, worksIncludes)).sortBy(_.id).toList

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
      val returnedWorks = result.hits.hits
        .map { jsonToIdentifiedWork(_.sourceAsString) }
        .map { DisplayWork(_) }
      returnedWorks.toList shouldBe expectedWorks
    }
  }

  private def jsonToIdentifiedWork(document: String): IdentifiedWork =
    fromJson[IdentifiedWork](document).get
}
