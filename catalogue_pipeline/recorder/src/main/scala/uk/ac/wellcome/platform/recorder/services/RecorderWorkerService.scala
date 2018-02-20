package uk.ac.wellcome.platform.recorder.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import io.circe.Decoder
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.sqs.{SQSReader, SQSWorkerToDynamo}

import scala.concurrent.Future

class RecorderWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorkerToDynamo[Work](reader, system, metrics) {

  override implicit val decoder = implicitly[Decoder[Work]]

  override def store(work: Work): Future[Unit] =
    Future.successful(())

}
