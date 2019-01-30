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
import uk.ac.wellcome.platform.archive.bagreplicator.models.errors.{
  BagReplicationError,
  DuplicationFailed,
  NotificationFailed,
  NotificationParsingFailed
}
import uk.ac.wellcome.platform.archive.bagreplicator.models.messages._
import uk.ac.wellcome.platform.archive.bagreplicator.storage.{
  BagStorage,
  S3Copier
}
import uk.ac.wellcome.platform.archive.common.flows.SupervisedMaterializer
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  NotificationMessage
}
import uk.ac.wellcome.platform.archive.common.progress.models._

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
      .map(notifyOutgoingTopic(outgoingSnsConfig))
      .map(notifyProgress(progressSnsConfig))
      .log("completed")

    messageStream.run("bag_replicator", flow)
  }

  private def parseReplicateBagMessage(notificationMessage: NotificationMessage)
    : Either[BagReplicationError, BagReplicationRequest] = {
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
    in: Either[BagReplicationError, BagReplicationRequest])(
    implicit s3Client: AmazonS3,
    s3Copier: S3Copier,
    ex: ExecutionContext)
    : Future[Either[BagReplicationError, CompletedBagReplication]] = {
    in.fold(
      left => Future(Left(left)),
      bagReplicationRequest =>
        BagStorage
          .duplicateBag(
            bagReplicationRequest.sourceBagLocation,
            storageDestination)
          .transformWith[Either[BagReplicationError, CompletedBagReplication]] {
            case Success(_) =>
              Future(
                Right(CompletedBagReplication(bagReplicationRequest.context)))
            case Failure(e) =>
              val stackTrace = e.getStackTrace.mkString("\n")

              error(
                List(
                  "Failed bag replication for",
                  bagReplicationRequest.context.bagLocation,
                  s"from ${bagReplicationRequest.sourceBagLocation}",
                  s"to ${storageDestination.namespace}/${storageDestination.rootPath}",
                  s"with error: ${e.getMessage}\n",
                  stackTrace
                ).mkString(" ")
              )
              Future(Left(
                DuplicationFailed(e.getMessage, bagReplicationRequest.context)))
        }
    )
  }

  private def notifyOutgoingTopic(outgoingSnsConfig: SNSConfig)(
    in: Either[BagReplicationError, CompletedBagReplication])(
    implicit encoder: Encoder[ArchiveComplete],
    snsClient: AmazonSNS) = {
    in.fold[Either[BagReplicationError, PublishedToOutgoingTopic]](
      bagReplicationError => Left(bagReplicationError),
      (completedBagReplication: CompletedBagReplication) =>
        publishNotification(completedBagReplication.context, outgoingSnsConfig) match {
          case Success(_) =>
            Right(PublishedToOutgoingTopic(completedBagReplication.context))
          case Failure(e) =>
            Left(
              NotificationFailed(e.getMessage, completedBagReplication.context))
      }
    )
  }

  private def notifyProgress(progressSnsConfig: SNSConfig)(
    in: Either[BagReplicationError, PublishedToOutgoingTopic])(
    implicit encoder: Encoder[ProgressUpdate],
    snsClient: AmazonSNS) = {
    in match {
      case Left(bagReplicationError) =>
        error(bagReplicationError.errorMessage)
        bagReplicationError match {
          case errorWithContext: BagReplicationError with BagReplicationContext =>
            publishNotification[ProgressUpdate](
              ProgressStatusUpdate(
                errorWithContext.context.archiveRequestId,
                Progress.Failed,
                None,
                List(ProgressEvent("Failed to replicate bag"))),
              progressSnsConfig)
          case error =>
            warn(s"Unable to notify progress for error without context $error")
        }
      case Right(PublishedToOutgoingTopic(archiveComplete)) =>
        val progressEventUpdate = ProgressEventUpdate(
          archiveComplete.archiveRequestId,
          List(ProgressEvent("Bag replicated successfully")))
        publishNotification[ProgressUpdate](
          progressEventUpdate,
          progressSnsConfig)
    }
    ()
  }

  private def publishNotification[T](msg: T, snsConfig: SNSConfig)(
    implicit encoder: Encoder[T],
    snsClient: AmazonSNS) = {
    toJson[T](msg)
      .map { messageString =>
        debug(s"snsPublishMessage: $messageString")
        new PublishRequest(snsConfig.topicArn, messageString, "bag_replicator")
      }
      .flatMap(publishRequest => Try(snsClient.publish(publishRequest)))
  }
}
