package uk.ac.wellcome.platform.api.services

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.SearchResponse

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
                  indexName: String,
                  limit: Int = 10,
                  from: Int = 0): Future[SearchResponse] =
    elasticClient
      .execute {
        search(s"$indexName/$documentType")
          .query(termQuery("type", "IdentifiedWork"))
          .sortBy(fieldSort(sortByField))
          .limit(limit)
          .from(from)
      }

  def simpleStringQueryResults(queryString: String,
                               limit: Int = 10,
                               from: Int = 0,
                               indexName: String): Future[SearchResponse] =
    elasticClient
      .execute {
        search(s"$indexName/$documentType")
          .query(
            must(
              simpleStringQuery(queryString),
              termQuery("type", "IdentifiedWork")
            ))
          .limit(limit)
          .from(from)
      }

}
