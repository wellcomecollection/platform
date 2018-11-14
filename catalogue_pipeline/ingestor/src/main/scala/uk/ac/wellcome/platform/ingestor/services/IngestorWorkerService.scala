package uk.ac.wellcome.platform.ingestor.services

import akka.actor.{ActorSystem, Terminated}
import com.amazonaws.services.sqs.model.Message
import com.google.inject.Inject
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork
import uk.ac.wellcome.platform.ingestor.IngestorConfig

import scala.concurrent.{ExecutionContext, Future}

class IngestorWorkerService @Inject()(
  ingestorConfig: IngestorConfig,
  identifiedWorkIndexer: WorkIndexer,
  messageStream: MessageStream[IdentifiedBaseWork],
  system: ActorSystem)(implicit ec: ExecutionContext) {

  case class MessageBundle(message: Message, work: IdentifiedBaseWork)

  val index = ingestorConfig.elasticConfig.indexName

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
    messageBundles: List[MessageBundle]): Future[List[MessageBundle]] = {
    for {
      works <- Future.successful(messageBundles.map(m => m.work))
      either <- identifiedWorkIndexer.indexWorks(
        works = works,
        indexName = index,
        documentType = ingestorConfig.elasticConfig.documentType
      )

    } yield {
      val failedWorks = either.left.getOrElse(Nil)
      messageBundles.filterNot {
        case MessageBundle(_, work) => failedWorks.contains(work)
      }
    }
  }

  def stop(): Future[Terminated] = {
    system.terminate()
  }

}
