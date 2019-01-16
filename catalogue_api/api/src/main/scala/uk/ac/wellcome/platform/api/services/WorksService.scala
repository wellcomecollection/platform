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

  def findWorkById(canonicalId: String)(
    index: Index): Future[Either[ElasticError, Option[IdentifiedBaseWork]]] =
    searchService
      .findResultById(canonicalId)(index)
      .map { result: Either[ElasticError, GetResponse] =>
        result.map { response: GetResponse =>
          if (response.exists)
            Some(jsonTo[IdentifiedBaseWork](response.sourceAsString))
          else None
        }
      }

  def listWorks(index: Index, worksSearchOptions: WorksSearchOptions)
    : Future[Either[ElasticError, ResultList]] =
    searchService
      .listResults(index, toElasticsearchQueryOptions(worksSearchOptions))
      .map { result: Either[ElasticError, SearchResponse] =>
        result.map { createResultList }
      }

  def searchWorks(query: String)(index: Index,
                                 worksSearchOptions: WorksSearchOptions)
    : Future[Either[ElasticError, ResultList]] =
    searchService
      .simpleStringQueryResults(query)(
        index,
        toElasticsearchQueryOptions(worksSearchOptions))
      .map { result: Either[ElasticError, SearchResponse] =>
        result.map { createResultList }
      }

  private def toElasticsearchQueryOptions(
    worksSearchOptions: WorksSearchOptions): ElasticsearchQueryOptions = {

    // Because we use Int for the pageSize and pageNumber, computing
    //
    //     from = (pageNumber - 1) * pageSize
    //
    // can potentially overflow and be negative or even wrap around.
    // For example, pageNumber=2018634700 and pageSize=100 would return
    // results if you don't handle this!
    //
    // If we are about to overflow, we pass the max possible int
    // into the Elasticsearch query and let it bubble up from there.
    // We could skip the query and throw here, because the user is almost
    // certainly doing something wrong, but that would mean simulating an
    // ES error or modifying our exception handling, and that seems more
    // disruptive than just clamping the overflow.
    //
    // Note: the checks on "pageSize" mean we know it must be
    // at most 100.

    // How this check works:
    //
    //    1.  If pageNumber > MaxValue, then (pageNumber - 1) * pageSize is
    //        probably bigger, as pageSize >= 1.
    //
    //    2.  Alternatively, we care about whether
    //
    //            pageSize * pageNumber > MaxValue
    //
    //        Since pageNumber is known positive, this is equivalent to
    //
    //            pageSize > MaxValue / pageNumber
    //
    //        And we know the division won't overflow because we have
    //        pageValue < MaxValue by the first check.
    //
    val willOverflow =
      (worksSearchOptions.pageNumber > Int.MaxValue) ||
        (worksSearchOptions.pageSize > Int.MaxValue / worksSearchOptions.pageNumber)

    val from = if (willOverflow) {
      Int.MaxValue
    } else {
      (worksSearchOptions.pageNumber - 1) * worksSearchOptions.pageSize
    }

    assert(
      from >= 0,
      message = s"from = $from < 0, which is daft.  Has something overflowed?"
    )

    ElasticsearchQueryOptions(
      filters = worksSearchOptions.filters,
      limit = worksSearchOptions.pageSize,
      from = from
    )
  }

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
