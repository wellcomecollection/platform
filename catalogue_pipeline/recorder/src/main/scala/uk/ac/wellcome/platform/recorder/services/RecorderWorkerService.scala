package uk.ac.wellcome.platform.recorder.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import io.circe.Decoder
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.UnidentifiedWork
import uk.ac.wellcome.sqs.{SQSReader, SQSWorkerToDynamo}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class RecorderWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorkerToDynamo[UnidentifiedWork](reader, system, metrics) {

  override implicit val decoder = implicitly[Decoder[UnidentifiedWork]]

  override def store(work: UnidentifiedWork): Future[Unit] =
    Future.successful(())

}
