package uk.ac.wellcome.platform.transformer.sierra.services

import grizzled.slf4j.Logging
import io.circe.ParsingFailure
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.message.MessageWriter
import uk.ac.wellcome.messaging.sns.{NotificationMessage, PublishAttempt}
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.sierra.exceptions.SierraTransformerException
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.HybridRecord

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class HybridRecordReceiver[T](
  messageWriter: MessageWriter[TransformedBaseWork],
  objectStore: ObjectStore[T])(implicit ec: ExecutionContext)
    extends Logging {

  def receiveMessage(
    message: NotificationMessage,
    transformToWork: (T, Int) => Try[TransformedBaseWork]): Future[Unit] = {
    debug(s"Starting to process message $message")

    val futurePublishAttempt = for {
      hybridRecord <- Future.fromTry(fromJson[HybridRecord](message.body))
      transformableRecord <- getTransformable(hybridRecord)
      work <- Future.fromTry(
        transformToWork(transformableRecord, hybridRecord.version))
      publishResult <- publishMessage(work)
      _ = debug(
        s"Published work: ${work.sourceIdentifier} with message $publishResult")
    } yield publishResult

    futurePublishAttempt
      .recover {
        case e: ParsingFailure =>
          info("Recoverable failure parsing HybridRecord from message", e)
          throw SierraTransformerException(e)
      }
      .map(_ => ())

  }

  private def getTransformable(hybridRecord: HybridRecord): Future[T] =
    objectStore.get(hybridRecord.location)

  private def publishMessage(
    work: TransformedBaseWork): Future[PublishAttempt] =
    messageWriter.write(
      message = work,
      subject = s"source: ${this.getClass.getSimpleName}.publishMessage"
    )
}
