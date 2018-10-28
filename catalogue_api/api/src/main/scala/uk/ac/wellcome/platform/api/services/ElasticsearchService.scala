package uk.ac.wellcome.platform.api.services

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.SearchDefinition
import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import com.sksamuel.elastic4s.searches.queries.term.TermQueryDefinition
import com.sksamuel.elastic4s.searches.sort.FieldSortDefinition
import uk.ac.wellcome.elasticsearch.DisplayElasticConfig
import uk.ac.wellcome.platform.api.models.ElasticsearchQueryOptions

import scala.concurrent.Future

@Singleton
class ElasticsearchService @Inject()(elasticClient: HttpClient,
                                     elasticConfig: DisplayElasticConfig) {

  val documentType: String = elasticConfig.documentType

  def findResultById(canonicalId: String,
                     indexName: String): Future[GetResponse] =
    elasticClient
      .execute {
        get(canonicalId).from(s"$indexName/$documentType")
      }

  def executeSearch(queryOptions: ElasticsearchQueryOptions): Future[SearchResponse] =
    elasticClient
      .execute { buildSearch(queryOptions) }

  /** Given a set of query options, but a SearchDefinition for Elasticsearch
    * using the elastic4s query DSL.
    *
    */
  private def buildSearch(queryOptions: ElasticsearchQueryOptions): SearchDefinition = {
    val queryDefinition = buildQuery(
      queryString = queryOptions.queryString,
      workType = queryOptions.workType
    )

    val sortDefinitions: List[FieldSortDefinition] =
      queryOptions.sortByField match {
        case Some(fieldName) => List(fieldSort(fieldName))
        case None => List()
      }

    search(s"${queryOptions.indexName}/$documentType")
      .query(queryDefinition)
      .sortBy(sortDefinitions)
      .limit(queryOptions.limit)
      .from(queryOptions.from)
  }

  private def buildQuery(queryString: Option[String],
                         workType: Option[String]): BoolQueryDefinition = {
    val queries = List(
      queryString.map { simpleStringQuery }
    ).flatten

    val filters: List[TermQueryDefinition] = List(
      workType.map { termQuery("workType.id", _) }
    ).flatten :+ termQuery("type", "IdentifiedWork")

    must(queries).filter(filters)
  }
}
