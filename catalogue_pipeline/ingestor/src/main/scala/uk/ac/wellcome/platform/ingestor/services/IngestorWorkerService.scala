package uk.ac.wellcome.platform.ingestor.services

import akka.Done
import com.amazonaws.services.sqs.model.Message
import com.sksamuel.elastic4s.http.ElasticClient
import grizzled.slf4j.Logging
import uk.ac.wellcome.elasticsearch.{ElasticsearchIndexCreator, WorksIndex}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork
import uk.ac.wellcome.platform.ingestor.config.models.IngestorConfig
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.{ExecutionContext, Future}

class IngestorWorkerService(
  elasticClient: ElasticClient,
  ingestorConfig: IngestorConfig,
  messageStream: MessageStream[IdentifiedBaseWork]
)(implicit
  ec: ExecutionContext)
    extends Runnable
    with Logging {

  type FutureBundles = Future[List[Bundle]]
  case class Bundle(message: Message, work: IdentifiedBaseWork)

  private val className = this.getClass.getSimpleName
  private val identifiedWorkIndexer = new WorkIndexer(elasticClient)
  private val indexCreator = new ElasticsearchIndexCreator(elasticClient)

  private val indexCreated = indexCreator.create(
    index = ingestorConfig.index,
    fields = WorksIndex.rootIndexFields
  )

  private def processMessages(bundles: List[Bundle]): FutureBundles =
    for {
      works <- Future.successful(bundles.map(m => m.work))
      either <- identifiedWorkIndexer.indexWorks(
        works = works,
        index = ingestorConfig.index
      )
    } yield {
      val failedWorks = either.left.getOrElse(Nil)
      bundles.filterNot {
        case Bundle(_, work) => failedWorks.contains(work)
      }
    }

  private def runStream(): Future[Done] = {
    messageStream.runStream(
      className,
      _.map { case (msg, work) => Bundle(msg, work) }
        .groupedWithin(
          ingestorConfig.batchSize,
          ingestorConfig.flushInterval
        )
        .mapAsyncUnordered(10) { msgs =>
          for { bundles <- processMessages(msgs.toList) } yield
            bundles.map(_.message)
        }
        .mapConcat(identity)
    )
  }

  def run(): Future[Done] =
    for {
      _ <- indexCreated
      result <- runStream()
    } yield result
}
