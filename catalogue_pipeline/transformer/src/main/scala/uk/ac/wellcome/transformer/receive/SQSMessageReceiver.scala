package uk.ac.wellcome.transformer.receive

import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.ParsingFailure
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.{
  CalmTransformable,
  MiroTransformable,
  SierraTransformable,
  Transformable
}
import uk.ac.wellcome.s3.SourcedObjectStore
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.transformer.transformers.{
  CalmTransformableTransformer,
  MiroTransformableTransformer,
  SierraTransformableTransformer,
  TransformableTransformer
}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.util.Try

class SQSMessageReceiver @Inject()(snsWriter: SNSWriter,
                                   sourcedObjectStore: SourcedObjectStore,
                                   metricsSender: MetricsSender)
    extends Logging {

  def receiveMessage(message: SQSMessage): Future[Unit] = {
    debug(s"Starting to process message $message")
    metricsSender.timeAndCount(
      "ingest-time",
      () => {
        val futurePublishAttempt = for {
          hybridRecord <- Future.fromTry(
            JsonUtil.fromJson[HybridRecord](message.body))
          transformableRecord <- getTransformable(hybridRecord)
          cleanRecord <- Future.fromTry(
            transformTransformable(transformableRecord))
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

  private def getTransformable(hybridRecord: HybridRecord) = {
    hybridRecord.sourceName match {
      case "miro" =>
        sourcedObjectStore.get[MiroTransformable](hybridRecord.s3key)
      case "calm" =>
        sourcedObjectStore.get[CalmTransformable](hybridRecord.s3key)
      case "sierra" =>
        sourcedObjectStore.get[SierraTransformable](hybridRecord.s3key)
    }
  }

  private def transformTransformable(
    transformable: Transformable): Try[Option[Work]] = {
    val transformableTransformer = chooseTransformer(transformable)
    transformableTransformer.transform(transformable) map { transformed =>
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
    maybeWork: Option[Work]): Future[Option[PublishAttempt]] =
    maybeWork.fold(Future.successful(None: Option[PublishAttempt])) { work =>
      snsWriter
        .writeMessage(
          message = JsonUtil.toJson(work).get,
          subject = s"source: ${this.getClass.getSimpleName}.publishMessage"
        )
        .map(publishAttempt => Some(publishAttempt))
    }
}
