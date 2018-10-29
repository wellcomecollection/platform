package uk.ac.wellcome.platform.api.services

import com.google.inject.{Inject, Singleton}
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

  def findWorkById(canonicalId: String)(
    documentOptions: ElasticsearchDocumentOptions)
    : Future[Option[IdentifiedBaseWork]] =
    searchService
      .findResultById(canonicalId)(documentOptions)
      .map { result =>
        if (result.exists)
          Some(jsonTo[IdentifiedBaseWork](result.sourceAsString))
        else None
      }

  def listWorks: (ElasticsearchDocumentOptions, WorksSearchOptions) => Future[ResultList] =
    executeSearch(
      searchService.listResults(sortByField = "canonicalId")
    )

  def searchWorks(query: String): (ElasticsearchDocumentOptions, WorksSearchOptions) => Future[ResultList] =
    executeSearch(
      searchService.simpleStringQueryResults(query)
    )

  private def executeSearch(
    searchRequest: (ElasticsearchDocumentOptions, ElasticsearchQueryOptions) => Future[SearchResponse]
  ): (ElasticsearchDocumentOptions, WorksSearchOptions) => Future[ResultList] =
    (documentOptions: ElasticsearchDocumentOptions, worksSearchOptions: WorksSearchOptions) => {
      val queryOptions = toElasticsearchQueryOptions(worksSearchOptions)
      searchRequest(documentOptions, queryOptions)
        .map { createResultList }
    }

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
      totalResults = searchResponse.totalHits
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
