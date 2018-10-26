package uk.ac.wellcome.platform.api.services

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.http.search.SearchHit
import io.circe.Decoder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.{IdentifiedBaseWork, IdentifiedWork}
import uk.ac.wellcome.platform.api.models.{ApiConfig, ResultList}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class WorksService @Inject()(
  apiConfig: ApiConfig,
  searchService: ElasticsearchService)(implicit ec: ExecutionContext) {

  def findWorkById(canonicalId: String,
                   indexName: String): Future[Option[IdentifiedBaseWork]] =
    searchService
      .findResultById(canonicalId, indexName = indexName)
      .map { result =>
        if (result.exists)
          Some(jsonTo[IdentifiedBaseWork](result.sourceAsString))
        else None
      }

  def listWorks(indexName: String,
                workType: Option[String] = None,
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
            jsonTo[IdentifiedWork](h.sourceAsString)
          }.toList,
          totalResults = searchResponse.totalHits
        )
      }

  def searchWorks(query: String,
                  workType: Option[String] = None,
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
            jsonTo[IdentifiedWork](h.sourceAsString)
          }.toList,
          totalResults = searchResponse.totalHits
        )
      }

  private def jsonTo[T <: IdentifiedBaseWork](document: String)(
    implicit decoder: Decoder[T]): T =
    fromJson[T](document) match {
      case Success(work) => work
      case Failure(e) =>
        throw new RuntimeException(
          s"Unable to parse JSON as Work ($e): $document"
        )
    }
}
