package uk.ac.wellcome.platform.transformer.receive

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.ParsingFailure
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.message.MessageWriter
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.transformable.{
  CalmTransformable,
  MiroTransformable,
  SierraTransformable,
  Transformable
}
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.transformer.GlobalExecutionContext.context
import uk.ac.wellcome.platform.transformer.transformers.{
  CalmTransformableTransformer,
  MiroTransformableTransformer,
  SierraTransformableTransformer
}
import uk.ac.wellcome.storage.s3.{S3Config, S3ObjectLocation, S3TypeStore}
import uk.ac.wellcome.storage.vhs.{HybridRecord, SourceMetadata}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.util.Try

class NotificationMessageReceiver @Inject()(
  messageWriter: MessageWriter[UnidentifiedWork],
  s3Client: AmazonS3,
  s3Config: S3Config,
  metricsSender: MetricsSender)
    extends Logging {

  def receiveMessage(message: NotificationMessage): Future[Unit] = {
    debug(s"Starting to process message $message")
    metricsSender.timeAndCount(
      "transform-time",
      () => {
        val futurePublishAttempt = for {
          hybridRecord <- Future.fromTry(
            fromJson[HybridRecord](message.Message))
          sourceMetadata <- Future.fromTry(
            fromJson[SourceMetadata](message.Message))
          transformableRecord <- getTransformable(hybridRecord, sourceMetadata)
          cleanRecord <- Future.fromTry(
            transformTransformable(transformableRecord, hybridRecord.version))
          publishResult <- publishMessage(cleanRecord)
        } yield publishResult

        futurePublishAttempt
          .recover {
            case e: ParsingFailure =>
              info("Recoverable failure parsing HybridRecord from message", e)
              throw GracefulFailureException(e)
          }
          .map(_ => ())
      }
    )
  }

  val miroTransformableStore =
    new S3TypeStore[MiroTransformable](s3Client)
  val calmTransformableStore =
    new S3TypeStore[CalmTransformable](s3Client)
  val sierraTransformableStore =
    new S3TypeStore[SierraTransformable](s3Client)

  private def getTransformable(
    hybridRecord: HybridRecord,
    sourceMetadata: SourceMetadata
  ) = {
    val s3ObjectLocation = S3ObjectLocation(
      bucket = s3Config.bucketName,
      key = hybridRecord.s3key
    )

    sourceMetadata.sourceName match {
      case "miro" => miroTransformableStore.get(s3ObjectLocation)
      case "calm" => calmTransformableStore.get(s3ObjectLocation)
      case "sierra" => sierraTransformableStore.get(s3ObjectLocation)
    }
  }

  private def transformTransformable(
    transformable: Transformable,
    version: Int
  ): Try[Option[UnidentifiedWork]] = {
    val transformableTransformer = chooseTransformer(transformable)
    transformableTransformer.transform(transformable, version) map {
      transformed =>
        debug(s"Transformed record to $transformed")
        transformed
    } recover {
      case e: Throwable =>
        error("Failed to perform transform to unified item", e)
        throw e
    }
  }

  private def chooseTransformer(transformable: Transformable) = {
    transformable match {
      case _: CalmTransformable => new CalmTransformableTransformer
      case _: MiroTransformable => new MiroTransformableTransformer
      case _: SierraTransformable => new SierraTransformableTransformer
    }
  }

  private def publishMessage(
    maybeWork: Option[UnidentifiedWork]): Future[Unit] =
    maybeWork.fold(Future.successful(())) { work =>
      messageWriter.write(
        work,
        s"source: ${this.getClass.getSimpleName}.publishMessage")
    }
}
