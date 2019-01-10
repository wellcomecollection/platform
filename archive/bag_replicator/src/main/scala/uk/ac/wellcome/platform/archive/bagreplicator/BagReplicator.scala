package uk.ac.wellcome.platform.archive.bagreplicator

import akka.Done
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import grizzled.slf4j.Logging
import io.circe.Encoder
import uk.ac.wellcome.Runnable
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.bagreplicator.config.BagReplicatorConfig
import uk.ac.wellcome.platform.archive.bagreplicator.models.StorageLocation
import uk.ac.wellcome.platform.archive.bagreplicator.models.errors.NotificationParsingFailed
import uk.ac.wellcome.platform.archive.bagreplicator.models.messages._
import uk.ac.wellcome.platform.archive.bagreplicator.storage.{BagStorage, S3Copier}
import uk.ac.wellcome.platform.archive.common.flows.SupervisedMaterializer
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.models.{ArchiveComplete, NotificationMessage}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class BagReplicator(
                     s3Client: AmazonS3,
                     snsClient: AmazonSNS,
                     messageStream: MessageStream[NotificationMessage, Unit],
                     bagReplicatorConfig: BagReplicatorConfig,
                     progressSnsConfig: SNSConfig,
                     outgoingSnsConfig: SNSConfig)(implicit val actorSystem: ActorSystem)
    extends Logging
    with Runnable {

  def run(): Future[Done] = {
    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")
    implicit val materializer: ActorMaterializer =
      SupervisedMaterializer.resumable
    implicit val s3client: AmazonS3 = s3Client
    implicit val amazonSNS: AmazonSNS = snsClient
    implicit val ex: ExecutionContext = actorSystem.dispatcher
    implicit val s3Copier: S3Copier = new S3Copier()

    val flow = Flow[NotificationMessage]
      .log("received notification message")
      .map(parseReplicateBagMessage)
      .mapAsync(bagReplicatorConfig.parallelism)(
        duplicateBagItems(bagReplicatorConfig.destination))
      .map(notifyOutgoingTopic(outgoingSnsConfig, "replicated"))
      .map(completeBagReplication)
      .log("completed")

    messageStream.run("bag_replicator", flow)
  }

  private def parseReplicateBagMessage(notificationMessage: NotificationMessage)
    : Either[Throwable, BagReplicationRequest[ArchiveComplete]] = {
    fromJson[ArchiveComplete](notificationMessage.body) match {
      case Success(archiveComplete) =>
        Right(
          BagReplicationRequest(archiveComplete, archiveComplete.bagLocation))
      case Failure(error) =>
        Left(NotificationParsingFailed(
          s"Failed to parse Notification error: $error body: ${notificationMessage.body}"))
    }
  }

  private def duplicateBagItems(storageDestination: StorageLocation)(
    in: Either[Throwable, BagReplicationRequest[ArchiveComplete]])(
    implicit s3Client: AmazonS3,
    s3Copier: S3Copier,
    ex: ExecutionContext)
    : Future[Either[Throwable, CompletedBagReplication[ArchiveComplete]]] = {
    in.fold(
      left => Future(Left(left)),
      bagReplicationRequest =>
        BagStorage
          .duplicateBag(
            bagReplicationRequest.sourceBagLocation,
            storageDestination)
          .transformWith[Either[Throwable,
                                CompletedBagReplication[ArchiveComplete]]] {
            case Success(_) =>
              Future(
                Right(CompletedBagReplication(bagReplicationRequest.context)))
            case Failure(e) => Future(Left(e))
        }
    )
  }

  private def notifyOutgoingTopic(snsConfig: SNSConfig, subject: String)(
    in: Either[Throwable, CompletedBagReplication[ArchiveComplete]])(
    implicit encoder: Encoder[ArchiveComplete], snsClient: AmazonSNS) = {
    in.fold[Either[Throwable, PublishedToOutgoingTopic[ArchiveComplete]]](
      left =>
        Left(left),
      (completedBagReplication: CompletedBagReplication[ArchiveComplete]) => {
        toJson[ArchiveComplete](completedBagReplication.context)
          .map { messageString =>
            debug(s"snsPublishMessage: $messageString")
            new PublishRequest(snsConfig.topicArn, messageString, subject)
          }.flatMap(publishRequest => Try(snsClient.publish(publishRequest)))
        match {
          case Success(_) => Right(PublishedToOutgoingTopic(completedBagReplication.context))
          case Failure(e) => Left(e)
        }
      })
  }

  private def completeBagReplication(in: Either[Throwable, PublishedToOutgoingTopic[ArchiveComplete]]): Unit = {
    in match {
      case Left(exception) =>
        error(exception.toString)
        ()
      case _ => ()
    }
  }
}
