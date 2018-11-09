package uk.ac.wellcome.platform.reindex.reindex_worker.services

import java.util

import akka.actor.{ActorSystem, Terminated}
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.google.inject.Inject
import com.gu.scanamo.{DynamoFormat, ScanamoFree}
import com.twitter.inject.annotations.Flag
import io.circe.Encoder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.reindex.reindex_worker.models.ReindexJob
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.reindex.reindex_worker.exceptions.ReindexerException
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, HybridRecord, VHSIndexEntry}

import scala.concurrent.{ExecutionContext, Future}

case class MiroMetadata(showInCatalogueAPI: Boolean)

class ReindexWorker @Inject()(
  recordReader: RecordReader,
  bulkSNSWriter: BulkSNSWriter,
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  @Flag("reindexer.tableMetadata") tableMetadata: String
)(implicit ec: ExecutionContext) {
  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  implicit val emptyMetadataDynamoFormat: DynamoFormat[EmptyMetadata] =
    DynamoFormat[EmptyMetadata]

  implicit val emptyMetadataEncoder: Encoder[EmptyMetadata] =
    Encoder[EmptyMetadata]
  implicit val miroMetadataEncoder: Encoder[MiroMetadata] =
    Encoder[MiroMetadata]

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      reindexJob: ReindexJob <- Future.fromTry(
        fromJson[ReindexJob](message.body))
      items <- recordReader.findRecordsForReindexing(reindexJob)
      _ <- tableMetadata match {
        case "MiroMetadata" => processMiroMetadata(items)
        case _              => processEmptyMetadata(items)
      }
    } yield ()

  def stop(): Future[Terminated] = system.terminate()

  /** These are the handlers that take the Dynamo rows, convert them into case
    * classes, and send them to SNS.
    *
    * We can't commonise this code any further, because we send different types
    * depending on whether the row has any metadata.
    *
    * In particular, we send a `HybridRecord` if the row doesn't have any
    * metadata, and `VHSIndexEntry` otherwise.
    */
  private def processEmptyMetadata(
    items: List[util.Map[String, AttributeValue]]): Future[Unit] =
    bulkSNSWriter
      .sendToSNS(items.map { av =>
        // AWLC: if you're changing this code to create an instance of
        // `EmptyMetadata` to include in a `VHSIndexEntry`, don't bother
        // parsing as a case class; just create a fresh value.
        //
        // When I tried to call `parseAsCaseClass[EmptyMetadata]`, I got
        // a NullPointerException.
        //
        parseAsCaseClass[HybridRecord](av)
      })
      .map { _ =>
        ()
      }

  private def processMiroMetadata(
    items: List[util.Map[String, AttributeValue]]): Future[Unit] =
    bulkSNSWriter
      .sendToSNS(
        items.map { av =>
          val hybridRecord = parseAsCaseClass[HybridRecord](av)
          val metadata = parseAsCaseClass[MiroMetadata](av)

          VHSIndexEntry(hybridRecord, metadata)
        }
      )
      .map { _ =>
        ()
      }

  /** Take the Map[String, AttributeValue], and convert it into an
    * instance of the case class `T`.  This is using a Scanamo helper --
    * I worked this out by looking at `ScanamoFree.get`.
    *
    * https://github.com/scanamo/scanamo/blob/12554b8e24ef8839d5e9dd9a4f42ae130e29b42b/scanamo/src/main/scala/com/gu/scanamo/ScanamoFree.scala#L62
    *
    */
  private def parseAsCaseClass[T](
    attributeValues: util.Map[String, AttributeValue])(
    implicit dynamoFormat: DynamoFormat[T]): T =
    ScanamoFree.read[T](attributeValues) match {
      case Right(t) => t
      case Left(err) =>
        throw ReindexerException(s"Error when parsing $attributeValues ($err)")
    }
}
