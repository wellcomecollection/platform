package uk.ac.wellcome.platform.api.services

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.{ElasticClient, Response}
import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.queries.{BoolQuery, Query}
import com.sksamuel.elastic4s.searches.queries.term.TermsQuery
import com.sksamuel.elastic4s.searches.sort.FieldSort
import uk.ac.wellcome.platform.api.models.{
  ItemLocationTypeFilter,
  WorkFilter,
  WorkTypeFilter
}

import scala.concurrent.Future

case class ElasticsearchDocumentOptions(
  indexName: String
)

case class ElasticsearchQueryOptions(
  filters: List[WorkFilter],
  limit: Int,
  from: Int
)

@Singleton
class ElasticsearchService @Inject()(elasticClient: ElasticClient) {
  def findResultById(canonicalId: String)(
    documentOptions: ElasticsearchDocumentOptions)
    : Future[Response[GetResponse]] =
    elasticClient
      .execute {
        get(canonicalId)
          .from(documentOptions.indexName)
      }

  def listResults(sortByField: String)
    : (ElasticsearchDocumentOptions,
       ElasticsearchQueryOptions) => Future[Response[SearchResponse]] =
    executeSearch(
      maybeQueryString = None,
      sortByField = Some(sortByField)
    )

  def simpleStringQueryResults(queryString: String)
    : (ElasticsearchDocumentOptions,
       ElasticsearchQueryOptions) => Future[Response[SearchResponse]] =
    executeSearch(
      maybeQueryString = Some(queryString),
      sortByField = None
    )

  /** Given a set of query options, build a SearchDefinition for Elasticsearch
    * using the elastic4s query DSL, then execute the search.
    */
  private def executeSearch(
    maybeQueryString: Option[String],
    sortByField: Option[String]
  )(documentOptions: ElasticsearchDocumentOptions,
    queryOptions: ElasticsearchQueryOptions)
    : Future[Response[SearchResponse]] = {
    val queryDefinition = buildQuery(
      maybeQueryString = maybeQueryString,
      filters = queryOptions.filters
    )

    val sortDefinitions: List[FieldSort] =
      sortByField match {
        case Some(fieldName) => List(fieldSort(fieldName))
        case None            => List()
      }

    val searchDefinition =
      search(documentOptions.indexName)
        .query(queryDefinition)
        .sortBy(sortDefinitions)
        .limit(queryOptions.limit)
        .from(queryOptions.from)

    elasticClient
      .execute { searchDefinition }
  }

  private def toTermQuery(workFilter: WorkFilter): TermsQuery[String] =
    workFilter match {
      case ItemLocationTypeFilter(itemLocationTypeIds) =>
        termsQuery(
          field = "items.agent.locations.locationType.id",
          values = itemLocationTypeIds)
      case WorkTypeFilter(workTypeIds) =>
        termsQuery(field = "workType.id", values = workTypeIds)
    }

  private def buildQuery(maybeQueryString: Option[String],
                         filters: List[WorkFilter]): BoolQuery = {
    val queries = List(
      maybeQueryString.map { simpleStringQuery }
    ).flatten

    val filterDefinitions
      : List[Query] = filters.map { toTermQuery } :+ termQuery(
      "type",
      "IdentifiedWork")

    must(queries).filter(filterDefinitions)
  }
}
