package uk.ac.wellcome.platform.ingestor.services

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.index.IndexResponse
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.exceptions.GracefulFailureException

import scala.util.{Failure, Success, Try}

@Singleton
class WorkIndexer @Inject()(
  @Flag("es.index") esIndex: String,
  @Flag("es.type") esType: String,
  elasticClient: HttpClient,
  metricsSender: MetricsSender
) extends Logging {

  def indexWork(document: String): Future[IndexResponse] = {
    implicit val jsonMapper = Work
    metricsSender.timeAndCount[IndexResponse](
      "ingestor-index-work",
      () => {
        Future
          .fromTry(
            fromJson[Work](document).recover{
              case error: Throwable => throw GracefulFailureException(error)
            }
          )
          .flatMap(item => {
            info(s"Indexing item $item")
            elasticClient.execute {
              indexInto(esIndex / esType).id(item.id).doc(item)
            }
          })
          .recover {
            case e: Throwable =>
              error(s"Error indexing document $document into Elasticsearch", e)
              throw e
          }
      }
    )
  }
}
