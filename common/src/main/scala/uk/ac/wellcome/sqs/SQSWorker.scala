package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.utils.JsonUtil.fromJson
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import scala.concurrent.Future
import com.twitter.inject.Logging
import scala.concurrent.duration._
import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.s3.S3Uri
import uk.ac.wellcome.s3.S3ObjectStore

abstract class SQSWorker(sqsReader: SQSReader,
                         actorSystem: ActorSystem,
                         metricsSender: MetricsSender,
                         s3: AmazonS3)
    extends Logging {

  info(s"Starting SQS worker=[$workerName]")

  lazy val poll = 1 second
  private lazy val workerName: String = this.getClass.getSimpleName
  private lazy val scheduler = actorSystem.scheduler
  private val actor = scheduler.schedule(0 seconds, poll)(processMessages())

  def processMessage(message: SQSMessage): Future[Unit]

  private def processMessages(): Future[Unit] = {
    sqsReader.retrieveAndDeleteMessages { message =>
      for {
        pointer <- Future.fromTry { fromJson[MessagePointer](message.getBody) }
        _ <- Future.successful {
          debug(s"Processing message pointer: $pointer")
        }
        message <- loadMessageFromStore(pointer)

        metricName = s"${workerName}_ProcessMessage"
        _ <- Future.successful { debug(s"Processing message: $message") }
        _ <- metricsSender.timeAndCount(
          metricName,
          () => processMessage(message))
      } yield ()
    } recover {
      case exception: Throwable => terminalFailureHook(exception)
    }
  }

  private def loadMessageFromStore(
    pointer: MessagePointer): Future[SQSMessage] = {
    pointer.src match {
      case S3Uri(bucket, key) => S3ObjectStore.get[SQSMessage](s3, bucket)(key)
      case _ =>
        Future.failed(new RuntimeException(
          s"Unsupported SQS message pointer scheme = [${pointer.src.getScheme}]"))
    }
  }

  def terminalFailureHook(throwable: Throwable): Unit = {
    logger.error(s"${workerName}_TerminalFailure!", throwable)
    metricsSender.incrementCount(s"${workerName}_TerminalFailure")
  }

  def stop(): Boolean = actor.cancel()

}
