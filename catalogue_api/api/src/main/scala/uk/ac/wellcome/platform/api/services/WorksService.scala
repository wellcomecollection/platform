package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.http.search.SearchHit
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.models.IdentifiedWork
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class ResultList(
  results: List[IdentifiedWork],
  totalResults: Int
)

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
                index: Option[String] = None): Future[ResultList] =
    searchService
      .listResults(
        sortByField = "canonicalId",
        limit = pageSize,
        from = (pageNumber - 1) * pageSize,
        index = index
      )
      .map { searchResponse =>
        ResultList(
          results = searchResponse.hits.hits.map { h: SearchHit =>
            jsonToIdentifiedWork(h.sourceAsString)
          }.toList,
          totalResults = searchResponse.totalHits
        )
      }

  def searchWorks(query: String,
                  pageSize: Int = defaultPageSize,
                  pageNumber: Int = 1,
                  index: Option[String] = None): Future[ResultList] =
    searchService
      .simpleStringQueryResults(
        query,
        limit = pageSize,
        from = (pageNumber - 1) * pageSize,
        index = index
      )
      .map { searchResponse =>
        ResultList(
          results = searchResponse.hits.hits.map { h: SearchHit =>
            jsonToIdentifiedWork(h.sourceAsString)
          }.toList,
          totalResults = searchResponse.totalHits
        )
      }

  private def jsonToIdentifiedWork(document: String): IdentifiedWork =
    fromJson[IdentifiedWork](document) match {
      case Success(work) => work
      case Failure(e) =>
        throw new RuntimeException(
          s"Unable to parse JSON as Work ($e): $document"
        )
    }
}
