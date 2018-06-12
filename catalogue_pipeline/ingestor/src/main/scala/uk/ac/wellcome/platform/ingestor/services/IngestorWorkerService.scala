package uk.ac.wellcome.platform.ingestor.services

import akka.actor.{ActorSystem, Terminated}
import com.amazonaws.services.sqs.model.Message
import com.google.inject.Inject
import uk.ac.wellcome.elasticsearch.ElasticConfig
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.models.work.internal.{IdentifiedWork, IdentifierType}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class IngestorWorkerService @Inject()(
  elasticConfig: ElasticConfig,
  identifiedWorkIndexer: WorkIndexer,
  messageStream: MessageStream[IdentifiedWork],
  system: ActorSystem)(implicit ec: ExecutionContext) {

  case class MessageBundle(message:Message, work: IdentifiedWork, indices: Set[String])

  messageStream.runStream { source =>
    source
      .map{case (message, identifiedWork) =>
        MessageBundle(message, identifiedWork, decideTargetIndices(identifiedWork))
      }
      .groupedWithin(100, 5 seconds).mapAsyncUnordered(10){ messages =>
        val futureSuccessfulWorks = processMessages(messages)
        futureSuccessfulWorks.map(sucessfulWorks => messages.filter{ case MessageBundle(message, identifiedWork, _) =>
          sucessfulWorks.contains(identifiedWork)
        }.map(_.message))
      }
      .mapConcat(identity)
  }

  private def processMessages(messageBundles: Seq[MessageBundle]): Future[Seq[IdentifiedWork]] =
    for {
      indicesToWorksMap <- Future.fromTry(Try(sortInTargetIndices(messageBundles)))
      listOfEither <-Future.sequence(indicesToWorksMap.map { case (index, sortedWorks) =>
          identifiedWorkIndexer.indexWorks(
            works = sortedWorks,
            esIndex = index,
            esType = elasticConfig.documentType
          )
        })

      } yield {
      listOfEither.partition(_.isLeft) match {
        case (indicesToLeftEithers, _) =>
          val failedWorks = indicesToLeftEithers.collect { case Left(works) => works }.flatten.toSeq
          messageBundles.filterNot{ case MessageBundle(_, work, _) => failedWorks.contains(work)}.map {_.work}
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
          elasticConfig.indexV1name,
          elasticConfig.indexV2name
        )
      case sierraIdentifier.id =>
        Set(elasticConfig.indexV2name)
      case _ =>
        throw GracefulFailureException(new RuntimeException(
          s"Cannot ingest work with identifierType: ${work.sourceIdentifier.identifierType}"))
    }

  }

  private def sortInTargetIndices(works: Seq[MessageBundle]): Map[String, Seq[IdentifiedWork]] =
    works.foldLeft(Map[String, Seq[IdentifiedWork]]()){case (resultMap, MessageBundle(_, work, indices)) =>
      val workUpdateMap = indices.map { index =>
        val existingWorks = resultMap.getOrElse(index, Nil)
        val updatedWorks = existingWorks :+ work
        (index, updatedWorks)
      }.toMap
      resultMap ++ workUpdateMap
    }
}
