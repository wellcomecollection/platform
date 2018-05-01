package uk.ac.wellcome.platform.ingestor.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.sqs.{SQSMessage, SQSReader, SQSWorker}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.work.internal.{IdentifiedWork, IdentifierSchemes}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class IngestorWorkerService @Inject()(
  @Flag("es.index.v1") esIndexV1: String,
  @Flag("es.index.v2") esIndexV2: String,
  identifiedWorkIndexer: WorkIndexer,
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorker(reader, system, metrics) {

  override def processMessage(message: SQSMessage): Future[Unit] =
    for {
      work <- Future.fromTry(fromJson[IdentifiedWork](message.body))
      indices = decideTargetIndices(work)
      _ <- Future.sequence(
        indices.map(identifiedWorkIndexer.indexWork(work, _)))
    } yield ()

  // This method returns the indices where a work is to be ingested.
  // * Miro works are indexed in both v1 and v2 indices.
  // * Sierra works are indexed only in the v2 index.
  // * Works from any other source are not expected so they are discarded.
  private def decideTargetIndices(work: IdentifiedWork): List[String] = {
    work.sourceIdentifier.identifierScheme match {
      case IdentifierSchemes.miroImageNumber => List(esIndexV1, esIndexV2)
      case IdentifierSchemes.sierraSystemNumber => List(esIndexV2)
      case _ =>
        throw GracefulFailureException(new RuntimeException(
          s"Cannot ingest work with identifierScheme: ${work.sourceIdentifier.identifierScheme}"))
    }

  }
}
