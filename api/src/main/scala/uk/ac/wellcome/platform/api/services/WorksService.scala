package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.platform.api.models.{DisplaySearch, DisplayWork}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

@Singleton
class WorksService @Inject()(@Flag("api.pageSize") defaultPageSize: Int,
                             searchService: ElasticSearchService) {

  def findWorkById(canonicalId: String,
                   includes: List[String] = Nil,
                   index: Option[String] = None): Future[Option[DisplayWork]] =
    searchService
      .findResultById(canonicalId, index = index)
      .map { result =>
        if (result.exists) Some(DisplayWork(result.original, includes)) else None
      }

  def listWorks(pageSize: Int = defaultPageSize,
                pageNumber: Int = 1,
                includes: List[String] = Nil,
                index: Option[String] = None): Future[DisplaySearch] =
    searchService
      .listResults(
        sortByField = "canonicalId",
        limit = pageSize,
        from = (pageNumber - 1) * pageSize,
        index = index
      )
      .map { DisplaySearch(_, pageSize, includes) }

  def searchWorks(query: String,
                  pageSize: Int = defaultPageSize,
                  pageNumber: Int = 1,
                  includes: List[String] = Nil,
                  index: Option[String] = None): Future[DisplaySearch] =
    searchService
      .simpleStringQueryResults(
        query,
        limit = pageSize,
        from = (pageNumber - 1) * pageSize,
        index = index
      )
      .map { DisplaySearch(_, pageSize, includes) }

}
