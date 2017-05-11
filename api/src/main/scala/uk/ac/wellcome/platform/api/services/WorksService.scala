package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.api.models.DisplayWork
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

case class PaginatedWorksResult(
  results: Array[DisplayWork],
  pageSize: Int,
  totalPages: Int,
  totalResults: Int
)

@Singleton
class WorksService @Inject()(
  searchService: ElasticSearchService
) {

  val defaultPageSize = 10
  val defaultStartPage = 1

  def findWorkById(canonicalId: String): Future[Option[DisplayWork]] =
    searchService
      .findResultById(canonicalId)
      .map { result =>
        if (result.exists) Some(DisplayWork(result.original)) else None
      }

  private def paginatedResult(searchResponse: RichSearchResponse,
                              pageSize: Int): PaginatedWorksResult =
    PaginatedWorksResult(
      results = searchResponse.hits.map { DisplayWork(_) },
      pageSize = pageSize,
      // TODO: This arithmetic is distinctly dodgy
      totalPages = (searchResponse.totalHits.toInt + pageSize) / pageSize,
      totalResults = searchResponse.totalHits.toInt
    )

  def findWorks(
    pageSize: Int = defaultPageSize,
    pageNumber: Int = defaultStartPage): Future[PaginatedWorksResult] =
    searchService
      .findResults(
        sortByField = "canonicalId",
        limit = pageSize,
        from = (pageNumber - 1) * pageSize
      )
      .map { paginatedResult(_, pageSize = pageSize) }

  def searchWorks(
    query: String,
    pageSize: Int = defaultPageSize,
    pageNumber: Int = defaultStartPage): Future[PaginatedWorksResult] =
    searchService
      .simpleStringQueryResults(
        query,
        limit = pageSize,
        from = (pageNumber - 1) * pageSize
      )
      .map { paginatedResult(_, pageSize = pageSize) }

}
