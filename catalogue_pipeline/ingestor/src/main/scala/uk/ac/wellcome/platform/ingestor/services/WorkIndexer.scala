package uk.ac.wellcome.platform.ingestor.services

import java.util.concurrent.TimeoutException

import com.sksamuel.elastic4s.Indexable
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import javax.inject.{Inject, Singleton}
import org.elasticsearch.client.ResponseException
import org.elasticsearch.index.VersionType
import uk.ac.wellcome.elasticsearch.ElasticsearchExceptionManager
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.work_model.IdentifiedWork

import scala.concurrent.Future

@Singleton
class WorkIndexer @Inject()(
  @Flag("es.type") esType: String,
  elasticClient: HttpClient,
  metricsSender: MetricsSender
) extends Logging
    with ElasticsearchExceptionManager {

  implicit object IdentifiedWorkIndexable extends Indexable[IdentifiedWork] {
    override def json(t: IdentifiedWork): String =
      toJson(t).get
  }

  def indexWork(work: IdentifiedWork, esIndex: String): Future[Any] = {

    // This is required for elastic4s, not Circe
    implicit val jsonMapper = IdentifiedWork

    metricsSender.timeAndCount[Any](
      "ingestor-index-work",
      () => {
        info(s"Indexing work ${work.canonicalId}")

        elasticClient
          .execute {
            indexInto(esIndex / esType)
              .version(work.version)
              .versionType(VersionType.EXTERNAL_GTE)
              .id(work.canonicalId)
              .doc(work)
          }
          .recover {
            case e: ResponseException
                if getErrorType(e).contains(
                  "version_conflict_engine_exception") =>
              warn(
                s"Trying to ingest work ${work.canonicalId} with older version: skipping.")
              ()
            case e: TimeoutException =>
              warn(
                s"Timeout indexing work ${work.canonicalId} into Elasticsearch")
              throw new GracefulFailureException(e)
            case e: Throwable =>
              error(
                s"Error indexing work ${work.canonicalId} into Elasticsearch",
                e)
              throw e
          }
      }
    )
  }
}
