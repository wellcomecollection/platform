package uk.ac.wellcome.platform.recorder.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveDecoder
import uk.ac.wellcome.messaging.message.{MessageReader, MessageWorker}
import uk.ac.wellcome.messaging.sqs.SQSReader
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.platform.recorder.models.RecorderWorkEntry
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.VersionedHybridStore
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

case class EmptyMetadata()

class RecorderWorkerService @Inject()(
  versionedHybridStore: VersionedHybridStore[RecorderWorkEntry],
  sqsReader: SQSReader,
  messageReader: MessageReader[UnidentifiedWork],
  system: ActorSystem,
  metrics: MetricsSender
) extends MessageWorker[UnidentifiedWork](messageReader, system, metrics) {

  implicit val decoder: Decoder[UnidentifiedWork] = deriveDecoder[UnidentifiedWork]

  override def processMessage(work: UnidentifiedWork): Future[Unit] = {
    val newRecorderEntry = RecorderWorkEntry(work)

    versionedHybridStore.updateRecord(newRecorderEntry.id)(newRecorderEntry)(
      existingEntry => if (existingEntry.work.version > newRecorderEntry.work.version) {
        existingEntry
      } else { newRecorderEntry }
    )(EmptyMetadata())
  }
}
