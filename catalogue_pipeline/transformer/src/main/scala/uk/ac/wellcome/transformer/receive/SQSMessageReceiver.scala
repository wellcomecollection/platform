package uk.ac.wellcome.transformer.receive

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.ParsingFailure
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{SourceMetadata, UnidentifiedWork}
import uk.ac.wellcome.models.aws.{S3Config, SQSMessage}
import uk.ac.wellcome.models.transformable.{
  CalmTransformable,
  MiroTransformable,
  SierraTransformable,
  Transformable
}
import uk.ac.wellcome.s3.S3ObjectStore
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.transformer.transformers.{
  CalmTransformableTransformer,
  MiroTransformableTransformer,
  SierraTransformableTransformer
}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.util.Try

class SQSMessageReceiver @Inject()(snsWriter: SNSWriter,
                                   s3Client: AmazonS3,
                                   s3Config: S3Config,
                                   metricsSender: MetricsSender)
    extends Logging {

  def receiveMessage(message: SQSMessage): Future[Unit] = {
    debug(s"Starting to process message $message")
    metricsSender.timeAndCount(
      "ingest-time",
      () => {
        val futurePublishAttempt = for {
          hybridRecord <- Future.fromTry(fromJson[HybridRecord](message.body))
          sourceMetadata <- Future.fromTry(
            fromJson[SourceMetadata](message.body))
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

  private def getTransformable(
    hybridRecord: HybridRecord,
    sourceMetadata: SourceMetadata
  ) = {
    sourceMetadata.sourceName match {
      case "miro" =>
        S3ObjectStore.get[MiroTransformable](s3Client, s3Config.bucketName)(
          hybridRecord.s3key)
      case "calm" =>
        S3ObjectStore.get[CalmTransformable](s3Client, s3Config.bucketName)(
          hybridRecord.s3key)
      case "sierra" =>
        S3ObjectStore.get[SierraTransformable](s3Client, s3Config.bucketName)(
          hybridRecord.s3key)
    }
  }

  private def transformTransformable(
    transformable: Transformable,
    version: Int): Try[Option[UnidentifiedWork]] = {
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

  private def publishMessage(maybeWork: Option[UnidentifiedWork]): Future[Unit] =
    Future.successful {
      maybeWork.map { work =>
        snsWriter
          .writeMessage(
            message = toJson(work).get,
            subject = s"source: ${this.getClass.getSimpleName}.publishMessage"
          )
      }.getOrElse(Unit)
  }
}
