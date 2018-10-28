package uk.ac.wellcome.platform.api.services

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchResponse}
import io.circe.Decoder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.{IdentifiedBaseWork, IdentifiedWork}
import uk.ac.wellcome.platform.api.models.{ApiConfig, ResultList}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class WorksSearchOptions(
  workTypeFilter: Option[String] = None,
  indexName: String,
  pageSize: Int = 10,
  pageNumber: Int = 1
)

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

  def listWorks(worksSearchOptions: WorksSearchOptions): Future[ResultList] =
    searchService
      .listResults(sortByField = "canonicalId")(toElasticsearchQueryOptions(worksSearchOptions))
      .map { createResultList }

  def searchWorks(query: String)(worksSearchOptions: WorksSearchOptions): Future[ResultList] =
    searchService
      .simpleStringQueryResults(query)(toElasticsearchQueryOptions(worksSearchOptions))
      .map { createResultList }

  private def toElasticsearchQueryOptions(worksSearchOptions: WorksSearchOptions): ElasticsearchQueryOptions =
    ElasticsearchQueryOptions(
      workTypeFilter = worksSearchOptions.workTypeFilter,
      indexName = worksSearchOptions.indexName,
      limit = worksSearchOptions.pageSize,
      from = (worksSearchOptions.pageNumber - 1) * worksSearchOptions.pageSize
    )

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
