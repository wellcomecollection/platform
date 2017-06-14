package uk.ac.wellcome.platform.ingestor.services

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.index.RichIndexResponse
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.IdentifiedWork
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

@Singleton
class IdentifiedWorkIndexer @Inject()(
  @Flag("es.index") esIndex: String,
  @Flag("es.type") esType: String,
  elasticClient: HttpClient,
  metricsSender: MetricsSender
) extends Logging {

  def indexIdentifiedWork(document: String): Future[RichIndexResponse] = {
    implicit val jsonMapper = IdentifiedWork

    metricsSender.timeAndCount[RichIndexResponse]("ingestor-index-work", () => {
      Future
        .fromTry(JsonUtil.fromJson[IdentifiedWork](document))
        .flatMap(item => {
          info(s"Indexing item $item")
          elasticClient.execute {
            indexInto(esIndex / esType).id(item.canonicalId).doc(item)
          }
        })
        .recover {
          case e: Throwable =>
            error(s"Error indexing document $document into Elasticsearch", e)
            throw e
        }
    })
  }
}
