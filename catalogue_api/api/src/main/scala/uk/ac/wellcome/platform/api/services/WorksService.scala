package uk.ac.wellcome.platform.api.services

import com.google.inject.Inject
import com.sksamuel.elastic4s.http.search.SearchHit
import javax.inject.Singleton

import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.platform.api.models.{ApiConfig, ResultList}
import uk.ac.wellcome.platform.api.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class WorksService @Inject()(apiConfig: ApiConfig,
                             searchService: ElasticSearchService) {

  def findWorkById(canonicalId: String,
                   indexName: String): Future[Option[IdentifiedWork]] =
    searchService
      .findResultById(canonicalId, indexName = indexName)
      .map { result =>
        if (result.exists)
          Some(jsonToIdentifiedWork(result.sourceAsString))
        else None
      }

  def listWorks(indexName: String,
                pageSize: Int = apiConfig.defaultPageSize,
                pageNumber: Int = 1): Future[ResultList] =
    searchService
      .listResults(
        sortByField = "canonicalId",
        limit = pageSize,
        from = (pageNumber - 1) * pageSize,
        indexName = indexName
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
                  indexName: String,
                  pageSize: Int = apiConfig.defaultPageSize,
                  pageNumber: Int = 1): Future[ResultList] =
    searchService
      .simpleStringQueryResults(
        query,
        limit = pageSize,
        from = (pageNumber - 1) * pageSize,
        indexName = indexName
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
