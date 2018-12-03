package uk.ac.wellcome.platform.api.services

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.{Index, IndexAndType}
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.{SearchRequest, SearchType}
import com.sksamuel.elastic4s.searches.queries.term.TermsQuery
import com.sksamuel.elastic4s.searches.queries.{BoolQuery, Query}
import com.sksamuel.elastic4s.searches.sort.{FieldSort, SortOrder}
import uk.ac.wellcome.platform.api.models.{
  ItemLocationTypeFilter,
  WorkFilter,
  WorkTypeFilter
}

import scala.concurrent.Future

// TODO: This is just IndexAndType
case class ElasticsearchDocumentOptions(
  index: Index,
  documentType: String
)

case class ElasticsearchQueryOptions(
  filters: List[WorkFilter],
  limit: Int,
  from: Int
)

@Singleton
class ElasticsearchService @Inject()(elasticClient: ElasticClient) {

  def findResultById(canonicalId: String)(
    indexAndType: IndexAndType): Future[GetResponse] =
    elasticClient
      .execute {
        get(canonicalId).from(indexAndType)
      }
      .map { _.result }

  def listResults: (Index,
                    ElasticsearchQueryOptions) => Future[SearchResponse] =
    executeSearch(
      maybeQueryString = None,
      sortDefinitions = List(fieldSort("canonicalId").order(SortOrder.ASC))
    )

  def simpleStringQueryResults(queryString: String)
    : (Index,
       ElasticsearchQueryOptions) => Future[SearchResponse] =
    executeSearch(
      maybeQueryString = Some(queryString),
      sortDefinitions = List(
        fieldSort("_score").order(SortOrder.DESC),
        fieldSort("canonicalId").order(SortOrder.ASC))
    )

  /** Given a set of query options, build a SearchDefinition for Elasticsearch
    * using the elastic4s query DSL, then execute the search.
    */
  private def executeSearch(
    maybeQueryString: Option[String],
    sortDefinitions: List[FieldSort]
  )(index: Index,
    queryOptions: ElasticsearchQueryOptions): Future[SearchResponse] = {
    val queryDefinition: BoolQuery = buildQuery(
      maybeQueryString = maybeQueryString,
      filters = queryOptions.filters
    )

    val searchRequest: SearchRequest =
      search(index)
        .searchType(SearchType.DFS_QUERY_THEN_FETCH)
        .query(queryDefinition)
        .sortBy(sortDefinitions)
        .limit(queryOptions.limit)
        .from(queryOptions.from)

    elasticClient
      .execute { searchRequest }
      .map { _.result }
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

    val filterDefinitions: List[Query] =
      filters.map { toTermQuery } :+ termQuery(
        field = "type",
        value = "IdentifiedWork")

    must(queries).filter(filterDefinitions)
  }
}
