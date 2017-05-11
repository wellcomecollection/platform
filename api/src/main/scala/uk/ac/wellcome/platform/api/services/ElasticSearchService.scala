package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.TcpClient
import com.sksamuel.elastic4s.get.RichGetResponse
import com.sksamuel.elastic4s.searches.{RichSearchResponse, SearchDefinition}
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

  private def executeSearch(searchDefinition: SearchDefinition, limit: Int, from: Int): Future[RichSearchResponse] =
    elasticClient
      .execute {
        searchDefinition
          .limit(limit)
          .from(from)
      }

  def listResults(sortByField: String,
                  limit: Int = 10,
                  from: Int = 0): Future[RichSearchResponse] = {
    val searchDefinition = search(s"$index/$itemType")
      .matchAllQuery()
      .sortBy(fieldSort(sortByField))
    executeSearch(
      searchDefinition = searchDefinition,
      limit = limit,
      from = from
    )
  }

  def simpleStringQueryResults(queryString: String, limit: Int = 10, from: Int = 0): Future[RichSearchResponse] = {
    val searchDefinition = search(s"$index/$itemType")
      .query(simpleStringQuery(queryString))
    executeSearch(
      searchDefinition = searchDefinition,
      limit = limit,
      from = from
    )
  }

}

