package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}

import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.models.IdentifiedWork
import uk.ac.wellcome.platform.api.models.{
  DisplayResultList,
  DisplayWork,
  WorksIncludes
}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class WorksService @Inject()(@Flag("api.pageSize") defaultPageSize: Int,
                             searchService: ElasticSearchService) {

  def findWorkById(canonicalId: String,
                   index: Option[String] = None): Future[Option[IdentifiedWork]] =
    searchService
      .findResultById(canonicalId, index = index)
      .map { result =>
        if (result.exists)
          Some(jsonToIdentifiedWork(result.sourceAsString))
        else None
      }

  def listWorks(pageSize: Int = defaultPageSize,
                pageNumber: Int = 1,
                includes: WorksIncludes = WorksIncludes(),
                index: Option[String] = None): Future[DisplayResultList] =
    searchService
      .listResults(
        sortByField = "canonicalId",
        limit = pageSize,
        from = (pageNumber - 1) * pageSize,
        index = index
      )
      .map { DisplayResultList(_, pageSize, includes) }

  def searchWorks(query: String,
                  pageSize: Int = defaultPageSize,
                  pageNumber: Int = 1,
                  includes: WorksIncludes = WorksIncludes(),
                  index: Option[String] = None): Future[DisplayResultList] =
    searchService
      .simpleStringQueryResults(
        query,
        limit = pageSize,
        from = (pageNumber - 1) * pageSize,
        index = index
      )
      .map { DisplayResultList(_, pageSize, includes) }

  private def jsonToIdentifiedWork(document: String): IdentifiedWork =
    fromJson[IdentifiedWork](document) match {
      case Success(work) => work
      case Failure(e) =>
        throw new RuntimeException(
          s"Unable to parse JSON as Work ($e): $document"
        )
    }
}
