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
class ElasticSearchService @Inject()(@Flag("es.index") defaultIndex: String,
                                     @Flag("es.type") documentType: String,
                                     elasticClient: TcpClient) {

  def findResultById(id: String,
                     index: String = defaultIndex): Future[RichGetResponse] =
    elasticClient
      .execute {
        get(id).from(s"$index/$documentType")
      }

  def listResults(sortByField: String,
                  limit: Int = 10,
                  from: Int = 0,
                  index: String = defaultIndex): Future[RichSearchResponse] =
    elasticClient
      .execute {
        search(s"$index/$documentType")
          .matchAllQuery()
          .sortBy(fieldSort(sortByField))
          .limit(limit)
          .from(from)
      }

  def simpleStringQueryResults(
    queryString: String,
    limit: Int = 10,
    from: Int = 0,
    index: String = defaultIndex): Future[RichSearchResponse] =
    elasticClient
      .execute {
        search(s"$index/$documentType")
          .query(simpleStringQuery(queryString))
          .limit(limit)
          .from(from)
      }

}
