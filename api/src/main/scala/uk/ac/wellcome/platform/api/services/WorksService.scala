package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.api.models.{DisplaySearch, DisplayWork}
import uk.ac.wellcome.platform.api.responses.ResultListResponse
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future


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


  def listWorks(
    pageSize: Int = defaultPageSize,
    pageNumber: Int = defaultStartPage): Future[DisplaySearch] =
    searchService
      .findResults(
        sortByField = "canonicalId",
        limit = pageSize,
        from = (pageNumber - 1) * pageSize
      )
      .map { DisplaySearch(_, pageSize) }

  def searchWorks(
    query: String,
    pageSize: Int = defaultPageSize,
    pageNumber: Int = defaultStartPage): Future[DisplaySearch] =
    searchService
      .simpleStringQueryResults(
        query,
        limit = pageSize,
        from = (pageNumber - 1) * pageSize
      )
      .map { DisplaySearch(_, pageSize) }

}
