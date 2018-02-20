package uk.ac.wellcome.platform.ingestor.services

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.index.IndexResponse
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import org.elasticsearch.index.VersionType
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

@Singleton
class WorkIndexer @Inject()(
  @Flag("es.index") esIndex: String,
  @Flag("es.type") esType: String,
  elasticClient: HttpClient,
  metricsSender: MetricsSender
) extends Logging {

  def indexWork(work: Work): Future[IndexResponse] = {

    // This is required for elastic4s, not Circe
    implicit val jsonMapper = Work

    metricsSender.timeAndCount[IndexResponse](
      "ingestor-index-work",
      () => {
        info(s"Indexing work ${work.id}")

        elasticClient
          .execute {
            indexInto(esIndex / esType).version(work.version).versionType(VersionType.EXTERNAL_GTE).id(work.id).doc(work)
          }
          .recover {
            case e: Throwable =>
              error(s"Error indexing work ${work.id} into Elasticsearch", e)
              throw e
          }
      }
    )
  }
}
