package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.TcpClient
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.platform.api.models._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

@Singleton
class ElasticSearchService @Inject()(@Flag("es.index") index: String,
                                     @Flag("es.type") itemType: String,
                                     elasticClient: TcpClient) {

  def findRecordById(canonicalId: String): Future[Option[Record]] =
    elasticClient
      .execute {
        get(canonicalId).from(s"$index/$itemType")
      }
      .map { result =>
        if (result.exists) Some(Record(result.original)) else None
      }

  def findRecords(): Future[Array[Record]] =
    elasticClient
      .execute {
        search(s"$index/$itemType")
          .matchAllQuery()
          // Sort so that we always have a consistent result that we can assert on
          .sortBy(fieldSort("canonicalId"))
          .limit(10)
      }
      .map { _.hits.map { Record(_) } }

}
