package uk.ac.wellcome.platform.api.services

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.http.ElasticError
import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchResponse}
import io.circe.Decoder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.{IdentifiedBaseWork, IdentifiedWork}
import uk.ac.wellcome.platform.api.models.{ResultList, WorkFilter}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class WorksSearchOptions(
  filters: List[WorkFilter],
  pageSize: Int,
  pageNumber: Int
)

@Singleton
class WorksService @Inject()(searchService: ElasticsearchService)(
  implicit ec: ExecutionContext) {

  def findWorkById(canonicalId: String)(index: Index): Future[Either[ElasticError, Option[IdentifiedBaseWork]]] =
    searchService
      .findResultById(canonicalId)(index)
      .map { result =>
        result.right.map { response: GetResponse =>
          if (response.exists)
            Some(jsonTo[IdentifiedBaseWork](response.sourceAsString))
          else None
        }
      }

  def listWorks(index: Index,
                worksSearchOptions: WorksSearchOptions): Future[Either[ElasticError, ResultList]] =
    searchService
      .listResults(
        index,
        toElasticsearchQueryOptions(worksSearchOptions))
      .map { _.right.map { createResultList } }

  def searchWorks(query: String)(
    index: Index,
    worksSearchOptions: WorksSearchOptions): Future[Either[ElasticError, ResultList]] =
    searchService
      .simpleStringQueryResults(query)(
        index,
        toElasticsearchQueryOptions(worksSearchOptions))
      .map { _.right.map { createResultList } }

  private def toElasticsearchQueryOptions(
    worksSearchOptions: WorksSearchOptions): ElasticsearchQueryOptions =
    ElasticsearchQueryOptions(
      filters = worksSearchOptions.filters,
      limit = worksSearchOptions.pageSize,
      from = (worksSearchOptions.pageNumber - 1) * worksSearchOptions.pageSize
    )

  private def createResultList(searchResponse: SearchResponse): ResultList =
    ResultList(
      results = searchResponseToWorks(searchResponse),
      totalResults = searchResponse.totalHits.toInt
    )

  private def searchResponseToWorks(
    searchResponse: SearchResponse): List[IdentifiedWork] =
    searchResponse.hits.hits.map { h: SearchHit =>
      jsonTo[IdentifiedWork](h.sourceAsString)
    }.toList

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
