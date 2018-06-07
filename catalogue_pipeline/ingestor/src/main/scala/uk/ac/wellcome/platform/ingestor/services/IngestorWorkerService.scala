package uk.ac.wellcome.platform.ingestor.services

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import uk.ac.wellcome.elasticsearch.ElasticConfig
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.models.work.internal.{IdentifiedWork, IdentifierType}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try

class IngestorWorkerService @Inject()(
  elasticConfig: ElasticConfig,
  identifiedWorkIndexer: WorkIndexer,
  messageStream: MessageStream[IdentifiedWork],
  system: ActorSystem)(implicit ec: ExecutionContext) {

  messageStream.runStream { source =>
    source.groupedWithin(100, 10 seconds).mapAsyncUnordered(10){messages =>
      val works = messages.map{case (_,identifiedWork) => identifiedWork}
      val processWorksFuture = processMessages(works)
      processWorksFuture.map(_ => messages.map(_._1))
      }
    .mapConcat(identity)
  }

  private def processMessages(works: Seq[IdentifiedWork]): Future[Unit] =
    for {
      indicesToWorksMap <- Future.fromTry(Try(sortInTargetIndices(works)))
      _ <-Future.sequence(indicesToWorksMap.map { case (index, sortedWorks) =>
          identifiedWorkIndexer.indexWorks(
            works = sortedWorks,
            esIndex = index,
            esType = elasticConfig.documentType
          )
        })

      } yield ()

  def stop(): Future[Terminated] = {
    system.terminate()
  }

  // This method returns the indices where a work is to be ingested.
  // * Miro works are indexed in both v1 and v2 indices.
  // * Sierra works are indexed only in the v2 index.
  // * Works from any other source are not expected so they are discarded.
  private def decideTargetIndices(work: IdentifiedWork): List[String] = {
    val miroIdentifier = IdentifierType("miro-image-number")
    val sierraIdentifier = IdentifierType("sierra-system-number")
    work.sourceIdentifier.identifierType.id match {
      case miroIdentifier.id =>
        List(
          elasticConfig.indexV1name,
          elasticConfig.indexV2name
        )
      case sierraIdentifier.id =>
        List(elasticConfig.indexV2name)
      case _ =>
        throw GracefulFailureException(new RuntimeException(
          s"Cannot ingest work with identifierType: ${work.sourceIdentifier.identifierType}"))
    }

  }

  private def sortInTargetIndices(works: Seq[IdentifiedWork]): Map[String, Seq[IdentifiedWork]] =
    works.foldLeft(Map[String, Seq[IdentifiedWork]]()){(resultMap, work) =>
      val indices = decideTargetIndices(work)
      val workUpdateMap = indices.map { index =>
        val existingWorks = resultMap.getOrElse(index, Nil)
        val updatedWorks = existingWorks :+ work
        (index, updatedWorks)
      }.toMap
      workUpdateMap ++ resultMap
    }
}
