package uk.ac.wellcome.platform.transformer.miro.receive

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.ParsingFailure
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.message.MessageWriter
import uk.ac.wellcome.messaging.sns.{NotificationMessage, PublishAttempt}
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException
import uk.ac.wellcome.platform.transformer.miro.MiroTransformableTransformer
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.storage.{ObjectLocation, ObjectStore}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class MiroNotificationMessageReceiver @Inject()(
  messageWriter: MessageWriter[TransformedBaseWork],
  s3Client: AmazonS3,
  s3Config: S3Config,
  miroTransformableTransformer: MiroTransformableTransformer)(
  implicit miroTransformableStore: ObjectStore[MiroTransformable],
  ec: ExecutionContext
) extends Logging {

  def receiveMessage(message: MiroTransformable): Future[Unit] = {
    debug(s"Starting to process message $message")

    val futurePublishAttempt = for {
      work <- Future.fromTry(
        transformTransformable(message, hybridRecord.version))
      publishResult <- publishMessage(work)
      _ = debug(
        s"Published work: ${work.sourceIdentifier} with message $publishResult")
    } yield publishResult

    futurePublishAttempt
      .recover {
        case e: ParsingFailure =>
          info("Recoverable failure parsing HybridRecord from message", e)
          throw TransformerException(e)
      }
      .map(_ => ())

  }

  private def getTransformable(
    hybridRecord: HybridRecord
  ) = {
    val s3ObjectLocation = ObjectLocation(
      namespace = s3Config.bucketName,
      key = hybridRecord.s3key
    )

    miroTransformableStore.get(s3ObjectLocation)
  }

  private def transformTransformable(
    transformable: MiroTransformable,
    version: Int
  ): Try[TransformedBaseWork] = {
    miroTransformableTransformer.transform(transformable, version) map {
      transformed =>
        debug(s"Transformed record to $transformed")
        transformed
    } recover {
      case e: Throwable =>
        error("Failed to perform transform to unified item", e)
        throw e
    }
  }

  private def publishMessage(
    work: TransformedBaseWork): Future[PublishAttempt] =
    messageWriter.write(
      message = work,
      subject = s"source: ${this.getClass.getSimpleName}.publishMessage"
    )
}
