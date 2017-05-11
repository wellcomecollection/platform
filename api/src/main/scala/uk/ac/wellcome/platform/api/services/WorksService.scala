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

  def findWorkById(canonicalId: String): Future[Option[DisplayWork]] =
    searchService
      .findResultById(canonicalId)
      .map { result =>
        if (result.exists) Some(DisplayWork(result.original)) else None
      }

  private def paginatedResult(searchResponse: RichSearchResponse): PaginatedWorksResult =
    PaginatedWorksResult(
      results = searchResponse.hits.map { DisplayWork(_) },
      pageSize = 10,
      // TODO: This arithmetic is distinctly dodgy
      totalPages = ((searchResponse.totalHits + 10L) / 10L).toInt,
      totalResults = (searchResponse.totalHits).toInt
    )

  def findWorks(): Future[PaginatedWorksResult] =
    searchService
      .findResults(sortByField = "canonicalId", limit = 10)
      .map { paginatedResult(_) }

  def searchWorks(query: String): Future[PaginatedWorksResult] =
    searchService
      .simpleStringQueryResults(query)
      .map { paginatedResult(_) }

}
