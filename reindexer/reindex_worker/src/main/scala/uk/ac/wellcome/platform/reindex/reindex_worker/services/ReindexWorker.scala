package uk.ac.wellcome.platform.reindex.reindex_worker.services

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import com.gu.scanamo.DynamoFormat
import com.twitter.inject.annotations.Flag
import io.circe.Encoder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.reindex.reindex_worker.models.ReindexJob
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VHSIndexEntry}

import scala.concurrent.{ExecutionContext, Future}

case class MiroMetadata(showInCatalogueAPI: Boolean)

class ReindexWorker @Inject()(
  recordReader: RecordReader,
  vhsIndexEntrySender: VHSIndexEntrySender,
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  @Flag("reindexer.tableMetadata") tableMetadata: String
)(implicit ec: ExecutionContext) {
  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  implicit val emptyMetadataDynamoFormat: DynamoFormat[EmptyMetadata] = DynamoFormat[EmptyMetadata]

  implicit val emptyMetadataEncoder: Encoder[EmptyMetadata] = Encoder[EmptyMetadata]
  implicit val miroMetadataEncoder: Encoder[MiroMetadata] = Encoder[MiroMetadata]

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      reindexJob: ReindexJob <- Future.fromTry(
        fromJson[ReindexJob](message.body))
      _ <- tableMetadata match {
        case "MiroMetadata" => processReindexJob[MiroMetadata](reindexJob)
        case _ => processReindexJob[EmptyMetadata](reindexJob)
      }
    } yield ()

  def stop(): Future[Terminated] = system.terminate()

  private def processReindexJob[M](reindexJob: ReindexJob)(implicit dynamoFormat: DynamoFormat[M], encoder: Encoder[VHSIndexEntry[M]]) =
    for {
      recordsToSend <- recordReader.findRecordsForReindexing[M](reindexJob)
      _ <- vhsIndexEntrySender.sendToSNS[VHSIndexEntry[M]](recordsToSend)
    } yield ()
}
