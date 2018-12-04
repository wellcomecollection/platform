package uk.ac.wellcome.platform.ingestor.services

import akka.Done
import com.amazonaws.services.sqs.model.Message
import com.sksamuel.elastic4s.http.ElasticClient
import uk.ac.wellcome.elasticsearch.{ElasticsearchIndexCreator, WorksIndex}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork
import uk.ac.wellcome.platform.ingestor.config.models.IngestorConfig

import scala.concurrent.{ExecutionContext, Future}

class IngestorWorkerService(elasticClient: ElasticClient,
                            ingestorConfig: IngestorConfig,
                            messageStream: MessageStream[IdentifiedBaseWork])(
  implicit ec: ExecutionContext) {

  case class MessageBundle(message: Message, work: IdentifiedBaseWork)

  val identifiedWorkIndexer = new WorkIndexer(
    elasticClient = elasticClient
  )

  val elasticsearchIndexCreator = new ElasticsearchIndexCreator(
    elasticClient = elasticClient
  )

  elasticsearchIndexCreator.create(
    indexName = ingestorConfig.elasticConfig.indexName,
    mappingDefinitionBuilder = WorksIndex
  )

  def run(): Future[Done] =
    messageStream.runStream(
      this.getClass.getSimpleName,
      source =>
        source
          .map {
            case (message, identifiedWork) =>
              MessageBundle(message, identifiedWork)
          }
          .groupedWithin(ingestorConfig.batchSize, ingestorConfig.flushInterval)
          .mapAsyncUnordered(10) { messages =>
            for {
              successfulMessageBundles <- processMessages(messages.toList)
            } yield successfulMessageBundles.map(_.message)
          }
          .mapConcat(identity)
    )

  private def processMessages(
    messageBundles: List[MessageBundle]): Future[List[MessageBundle]] =
    for {
      works <- Future.successful(messageBundles.map(m => m.work))
      either <- identifiedWorkIndexer.indexWorks(
        works = works,
        indexName = ingestorConfig.elasticConfig.indexName
      )
    } yield {
      val failedWorks = either.left.getOrElse(Nil)
      messageBundles.filterNot {
        case MessageBundle(_, work) => failedWorks.contains(work)
      }
    }
}
