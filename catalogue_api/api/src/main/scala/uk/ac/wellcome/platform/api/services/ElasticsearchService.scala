package uk.ac.wellcome.platform.api.services

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.SearchDefinition
import com.sksamuel.elastic4s.searches.queries.term.TermsQueryDefinition
import com.sksamuel.elastic4s.searches.queries.{BoolQueryDefinition, QueryDefinition}
import com.sksamuel.elastic4s.searches.sort.FieldSortDefinition
import org.elasticsearch.search.sort.SortOrder
import uk.ac.wellcome.platform.api.models.{ItemLocationTypeFilter, WorkFilter, WorkTypeFilter}

import scala.concurrent.Future

case class ElasticsearchDocumentOptions(
  indexName: String,
  documentType: String
)

case class ElasticsearchQueryOptions(
  filters: List[WorkFilter],
  limit: Int,
  from: Int
)

@Singleton
class ElasticsearchService @Inject()(elasticClient: HttpClient) {

  def findResultById(canonicalId: String)(
    documentOptions: ElasticsearchDocumentOptions): Future[GetResponse] =
    elasticClient
      .execute {
        get(canonicalId).from(
          s"${documentOptions.indexName}/${documentOptions.documentType}")
      }

  def listResults: (ElasticsearchDocumentOptions,
                    ElasticsearchQueryOptions) => Future[SearchResponse] =
    executeSearch(
      maybeQueryString = None,
      sortDefinitions = List(fieldSort("canonicalId").order(SortOrder.ASC))
    )

  def simpleStringQueryResults(queryString: String)
    : (ElasticsearchDocumentOptions,
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
    sortDefinitions: List[FieldSortDefinition]
  )(documentOptions: ElasticsearchDocumentOptions,
    queryOptions: ElasticsearchQueryOptions): Future[SearchResponse] = {
    val queryDefinition = buildQuery(
      maybeQueryString = maybeQueryString,
      filters = queryOptions.filters
    )

    val searchDefinition: SearchDefinition =
      search(s"${documentOptions.indexName}/${documentOptions.documentType}")
        .query(queryDefinition)
        .sortBy(sortDefinitions)
        .limit(queryOptions.limit)
        .from(queryOptions.from)

    elasticClient
      .execute { searchDefinition }
  }

  private def toTermQuery(
    workFilter: WorkFilter): TermsQueryDefinition[String] =
    workFilter match {
      case ItemLocationTypeFilter(itemLocationTypeIds) =>
        termsQuery(
          field = "items.agent.locations.locationType.id",
          values = itemLocationTypeIds)
      case WorkTypeFilter(workTypeIds) =>
        termsQuery(field = "workType.id", values = workTypeIds)
    }

  private def buildQuery(maybeQueryString: Option[String],
                         filters: List[WorkFilter]): BoolQueryDefinition = {
    val queries = List(
      maybeQueryString.map { simpleStringQuery }
    ).flatten

    val filterDefinitions
      : List[QueryDefinition] = filters.map { toTermQuery } :+ termQuery(
      "type",
      "IdentifiedWork")

    must(queries).filter(filterDefinitions)
  }
}
