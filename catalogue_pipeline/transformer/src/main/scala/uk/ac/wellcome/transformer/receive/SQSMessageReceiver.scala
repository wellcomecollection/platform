package uk.ac.wellcome.transformer.receive

import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.ParsingFailure
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.s3.VersionedObjectStore
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.transformer.transformers.TransformableTransformer
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.util.Try

class SQSMessageReceiver @Inject()(
  snsWriter: SNSWriter,
  versionedObjectStore: VersionedObjectStore,
  transformableTransformer: TransformableTransformer[Transformable],
  metricsSender: MetricsSender)
    extends Logging {

  def receiveMessage(message: SQSMessage): Future[Unit] = {
    info(s"Starting to process message $message")
    metricsSender.timeAndCount(
      "ingest-time",
      () => {
        val futurePublishAttempt = for {
          hybridRecord <- Future.fromTry(
            JsonUtil.fromJson[HybridRecord](message.body))
          transformableRecord <- versionedObjectStore.get[Transformable](
            hybridRecord.s3key)
          cleanRecord <- Future.fromTry(
            transformTransformable(transformableRecord))
          publishResult <- publishMessage(cleanRecord)
        } yield publishResult

        futurePublishAttempt
          .recover {
            case e: ParsingFailure =>
              info("Recoverable failure extracting workfrom record", e)
              throw GracefulFailureException(e)
          }
          .map(_ => ())
      }
    )
  }

  private def transformTransformable(
    transformable: Transformable): Try[Option[Work]] = {
    transformableTransformer.transform(transformable) map { transformed =>
      info(s"Transformed record $transformed")
      transformed
    } recover {
      case e: Throwable =>
        error("Failed to perform transform to unified item", e)
        throw e
    }
  }

  private def publishMessage(
    maybeWork: Option[Work]): Future[Option[PublishAttempt]] =
    maybeWork.fold(Future.successful(None: Option[PublishAttempt])) { work =>
      snsWriter
        .writeMessage(JsonUtil.toJson(work).get, Some("Foo"))
        .map(publishAttempt => Some(publishAttempt))
    }
}
