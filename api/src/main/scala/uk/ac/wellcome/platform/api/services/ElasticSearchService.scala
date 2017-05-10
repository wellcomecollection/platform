package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.TcpClient
import com.sksamuel.elastic4s.get.RichGetResponse
import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.platform.api.models._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

@Singleton
class ElasticSearchService @Inject()(@Flag("es.index") index: String,
                                     @Flag("es.type") itemType: String,
                                     elasticClient: TcpClient) {

  def findResultById(id: String): Future[RichGetResponse] =
    elasticClient
      .execute {
        get(id).from(s"$index/$itemType")
      }

  def findResults(sortByField: String, limit: Int = 10): Future[RichSearchResponse] =
    elasticClient
      .execute {
        search(s"$index/$itemType")
          .matchAllQuery()
          .sortBy(fieldSort(sortByField))
          .limit(limit)
      }

  def simpleStringQueryResults(queryString: String): Future[RichSearchResponse] =
    elasticClient
      .execute {
        search(s"$index/$itemType")
          .query(simpleStringQuery(queryString))
          .limit(10)
      }

}

