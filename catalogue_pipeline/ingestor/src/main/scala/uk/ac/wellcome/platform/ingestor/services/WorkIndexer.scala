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
        info(s"Indexing work $work")

        // If the work has visible = false, we save a stub into Elasticsearch.
        // This record isn't searchable, but allows us to return proper
        // HTTP 410 Gone codes from the API.
        //
        // Longer term, this will allow us to track if/when a work was deleted,
        // and ensure that if we get an UPDATE and a DELETE out-of-order, we
        // don't restore a record that should really be deleted.
        val workToIndex = if (work.visible) {
          work
        } else {
          val stubWork = Work(
            canonicalId = work.canonicalId,
            sourceIdentifier = work.sourceIdentifier,
            identifiers = work.identifiers,
            title = "This work has been deleted",
            visible = false
          )
          info(s"Replacing work with stub record $stubWork")
          stubWork
        }

        elasticClient.execute {
          indexInto(esIndex / esType).id(workToIndex.id).doc(workToIndex)
        }.recover {
          case e: Throwable =>
            error(s"Error indexing work $work into Elasticsearch", e)
            throw e
        }
      }
    )
  }
}
