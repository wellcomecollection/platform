package uk.ac.wellcome.platform.api.services

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchResponse}
import io.circe.Decoder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.{IdentifiedBaseWork, IdentifiedWork}
import uk.ac.wellcome.platform.api.models.{ApiConfig, ElasticsearchQueryOptions, ResultList}

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
      .map { (result: GetResponse) =>
        if (result.exists)
          Some(jsonTo[IdentifiedBaseWork](result.sourceAsString))
        else None
      }

  def listWorks(indexName: String,
                workType: Option[String] = None,
                pageSize: Int = apiConfig.defaultPageSize,
                pageNumber: Int = 1): Future[ResultList] = {
    val queryOptions = ElasticsearchQueryOptions(
      sortByField = Option("canonicalId"),
      workType = workType,
      limit = pageSize,
      from = (pageNumber - 1) * pageSize,
      indexName = indexName
    )

    searchService
      .executeSearch(queryOptions)
      .map { createResultList }
  }

  def searchWorks(query: String,
                  workType: Option[String] = None,
                  indexName: String,
                  pageSize: Int = apiConfig.defaultPageSize,
                  pageNumber: Int = 1): Future[ResultList] = {
    val queryOptions = ElasticsearchQueryOptions(
      queryString = Some(query),
      workType = workType,
      limit = pageSize,
      from = (pageNumber - 1) * pageSize,
      indexName = indexName
    )

    searchService
      .executeSearch(queryOptions)
      .map { createResultList }
  }

  private def createResultList(searchResponse: SearchResponse): ResultList =
    ResultList(
      results = searchResponseToWorks(searchResponse),
      totalResults = searchResponse.totalHits
    )

  private def searchResponseToWorks(searchResponse: SearchResponse): List[IdentifiedWork] =
    searchResponse
      .hits.hits
      .map { h: SearchHit => jsonTo[IdentifiedWork](h.sourceAsString)}
      .toList

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
