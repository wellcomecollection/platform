package uk.ac.wellcome.platform.api.services

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import com.sksamuel.elastic4s.searches.queries.term.TermQueryDefinition
import uk.ac.wellcome.elasticsearch.DisplayElasticConfig

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

  def listResults(sortByField: String,
                  workType: Option[String] = None,
                  indexName: String,
                  limit: Int = 10,
                  from: Int = 0): Future[SearchResponse] =
    elasticClient
      .execute {
        search(s"$indexName/$documentType")
          .query(
            buildQuery(
              queryString = None,
              workType = workType
            )
          )
          .sortBy(fieldSort(sortByField))
          .limit(limit)
          .from(from)
      }

  def simpleStringQueryResults(queryString: String,
                               workType: Option[String] = None,
                               limit: Int = 10,
                               from: Int = 0,
                               indexName: String): Future[SearchResponse] =
    elasticClient
      .execute {
        search(s"$indexName/$documentType")
          .query(
            buildQuery(
              queryString = Some(queryString),
              workType = workType
            )
          )
          .limit(limit)
          .from(from)
      }

  private def buildQuery(queryString: Option[String], workType: Option[String]): BoolQueryDefinition = {
    val queries = List(
      queryString.map { simpleStringQuery }
    ).flatten

    val filters: List[TermQueryDefinition] = List(
      workType.map { termQuery("workType.id", _) }
    ).flatten :+ termQuery("type", "IdentifiedWork")

    must(queries).filter(filters)
  }
}
