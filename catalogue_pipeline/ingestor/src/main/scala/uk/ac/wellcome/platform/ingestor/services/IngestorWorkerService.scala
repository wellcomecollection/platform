package uk.ac.wellcome.platform.ingestor.services

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import uk.ac.wellcome.elasticsearch.ElasticConfig
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.models.work.internal.{IdentifiedWork, IdentifierType}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class IngestorWorkerService @Inject()(
  elasticConfig: ElasticConfig,
  identifiedWorkIndexer: WorkIndexer,
  messageStream: MessageStream[IdentifiedWork],
  system: ActorSystem)(implicit ec: ExecutionContext) {

  messageStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(work: IdentifiedWork): Future[Unit] = {
    val futureIndices: Future[List[String]] =
      Future.fromTry(Try(decideTargetIndices(work)))
    futureIndices.flatMap { indices =>
      val futureResults = indices.map { esIndex =>
        identifiedWorkIndexer.indexWork(
          work = work,
          esIndex = esIndex,
          esType = elasticConfig.documentType
        )
      }
      Future.sequence(futureResults).map { _ =>
        ()
      }
    }
  }
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
}
