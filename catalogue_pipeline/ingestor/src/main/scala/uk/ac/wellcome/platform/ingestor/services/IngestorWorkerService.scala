package uk.ac.wellcome.platform.ingestor.services

import akka.actor.{ActorSystem, Terminated}
import com.amazonaws.services.sqs.model.Message
import com.google.inject.Inject
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.models.work.internal.{IdentifiedWork, IdentifierType}
import uk.ac.wellcome.platform.ingestor.IngestorConfig

import scala.concurrent.{ExecutionContext, Future}

class IngestorWorkerService @Inject()(
  ingestorConfig: IngestorConfig,
  identifiedWorkIndexer: WorkIndexer,
  messageStream: MessageStream[IdentifiedWork],
  system: ActorSystem)(implicit ec: ExecutionContext) {

  case class MessageBundle(message: Message,
                           work: IdentifiedWork,
                           indices: Set[String])

  messageStream.runStream(
    this.getClass.getSimpleName,
    source =>
      source
        .map {
          case (message, identifiedWork) =>
            MessageBundle(
              message,
              identifiedWork,
              decideTargetIndices(identifiedWork))
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
      indicesToWorksMap <- Future.successful(
        sortInTargetIndices(messageBundles))
      listOfEither <- Future.sequence(indicesToWorksMap.map {
        case (index, sortedWorks) =>
          identifiedWorkIndexer.indexWorks(
            works = sortedWorks,
            esIndex = index,
            esType = ingestorConfig.elasticConfig.documentType
          )
      })

    } yield {
      val failedWorks =
        listOfEither.collect { case Left(works) => works }.flatten.toList
      messageBundles.filterNot {
        case MessageBundle(_, work, _) => failedWorks.contains(work)
      }
    }

  def stop(): Future[Terminated] = {
    system.terminate()
  }

  // This method returns the indices where a work is to be ingested.
  // * Miro works are indexed in both v1 and v2 indices.
  // * Sierra works are indexed only in the v2 index.
  // * Works from any other source are not expected so they are discarded.
  private def decideTargetIndices(work: IdentifiedWork): Set[String] = {
    val miroIdentifier = IdentifierType("miro-image-number")
    val sierraIdentifier = IdentifierType("sierra-system-number")
    work.sourceIdentifier.identifierType.id match {
      case miroIdentifier.id =>
        Set(
          ingestorConfig.elasticConfig.indexV1name,
          ingestorConfig.elasticConfig.indexV2name
        )
      case sierraIdentifier.id =>
        Set(ingestorConfig.elasticConfig.indexV2name)
      case _ =>
        throw GracefulFailureException(new RuntimeException(
          s"Cannot ingest work with identifierType: ${work.sourceIdentifier.identifierType}"))
    }

  }

  private def sortInTargetIndices(
    works: List[MessageBundle]): Map[String, List[IdentifiedWork]] =
    works.foldLeft(Map[String, List[IdentifiedWork]]()) {
      case (resultMap, MessageBundle(_, work, indices)) =>
        val workUpdateMap = indices.map { index =>
          val existingWorks = resultMap.getOrElse(index, Nil)
          val updatedWorks = existingWorks :+ work
          (index, updatedWorks)
        }.toMap
        resultMap ++ workUpdateMap
    }
}
