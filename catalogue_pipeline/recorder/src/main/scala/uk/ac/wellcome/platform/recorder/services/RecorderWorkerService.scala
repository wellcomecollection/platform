package uk.ac.wellcome.platform.recorder.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import com.gu.scanamo.DynamoFormat
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.messaging.sqs.{SQSReader, SQSWorkerToDynamo}
import uk.ac.wellcome.models.SourceMetadata
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.recorder.models.RecorderWorkEntry
import uk.ac.wellcome.storage.vhs.VersionedHybridStore
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class RecorderWorkerService @Inject()(
  versionedHybridStore: VersionedHybridStore[RecorderWorkEntry],
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender
) extends SQSWorkerToDynamo[UnidentifiedWork](reader, system, metrics) {

  implicit val decoder: Decoder[UnidentifiedWork] = Decoder[UnidentifiedWork]
  implicit val encoder: Encoder[UnidentifiedWork] = Encoder[UnidentifiedWork]

  override def store(work: UnidentifiedWork): Future[Unit] = {

    val newRecorderEntry = RecorderWorkEntry(work)

    versionedHybridStore.updateRecord(newRecorderEntry.id)(newRecorderEntry)(
      existingEntry => if (existingEntry.work.version > newRecorderEntry.work.version) {
        existingEntry
      } else { newRecorderEntry }
    )(SourceMetadata(sourceName = "transformer"))
  }
}
