package uk.ac.wellcome.platform.ingestor.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import com.twitter.inject.annotations.Flag
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.message.{MessageReader, MessageWorker}
import uk.ac.wellcome.messaging.metrics.MetricsSender
import uk.ac.wellcome.messaging.sqs.SQSReader
import uk.ac.wellcome.models.work.internal.{IdentifiedWork, IdentifierSchemes}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.util.Try

class IngestorWorkerService @Inject()( @Flag("es.index.v1") esIndexV1: String,
                                       @Flag("es.index.v2") esIndexV2: String,
                                       identifiedWorkIndexer: WorkIndexer,
                                       sqsReader: SQSReader,
                                       messageReader: MessageReader[IdentifiedWork],
                                       system: ActorSystem,
                                       metrics: MetricsSender
) extends MessageWorker[IdentifiedWork](sqsReader, messageReader, system, metrics) {

  override def processMessage(work: IdentifiedWork): Future[Unit] = {
      val futureIndices: Future[List[String]] = Future.fromTry(Try(decideTargetIndices(work)))
      futureIndices.flatMap( indices => {
        val futureResults = indices.map(identifiedWorkIndexer.indexWork(work, _))
        Future.sequence(futureResults).map { _ => () }
      })
  }

  override implicit val decoder: Decoder[IdentifiedWork] = deriveDecoder[IdentifiedWork]

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
