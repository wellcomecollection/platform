package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.twitter.inject.annotations.Flag
import javax.inject.{Inject, Singleton}
import uk.ac.wellcome.elasticsearch.{ElasticClientBuilder, ElasticClientConfig}

import scala.concurrent.Future

@Singleton
class ElasticSearchService @Inject()(@Flag("es.type") documentType: String,
                                     elasticClientConfig: ElasticClientConfig) {

  val elasticClient: HttpClient = ElasticClientBuilder.buildHttpClient(elasticClientConfig)

  def findResultById(canonicalId: String,
                     indexName: String): Future[GetResponse] =
    elasticClient
      .execute {
        get(canonicalId).from(s"${indexName}/$documentType")
      }

  def listResults(sortByField: String,
                  indexName: String,
                  limit: Int = 10,
                  from: Int = 0): Future[SearchResponse] =
    elasticClient
      .execute {
        search(s"${indexName}/$documentType")
          .query(termQuery("visible", true))
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
        search(s"${indexName}/$documentType")
          .query(
            must(
              simpleStringQuery(queryString),
              termQuery("visible", true)
            ))
          .limit(limit)
          .from(from)
      }

}
