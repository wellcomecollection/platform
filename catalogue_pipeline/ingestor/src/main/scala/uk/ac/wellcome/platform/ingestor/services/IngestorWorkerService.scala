package uk.ac.wellcome.platform.ingestor.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{IdentifiedWork, IdentifierSchemes}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
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
      _ <- Future.sequence(indices.map(identifiedWorkIndexer.indexWork(work, _)))
    } yield ()

  private def decideTargetIndices(work: IdentifiedWork): List[String] = {
    List(esIndexV1, esIndexV2)
  }
}
