package uk.ac.wellcome.platform.sierra_reader.services

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectResult
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.messaging.sqs._
import uk.ac.wellcome.platform.sierra_reader.flow.SierraRecordWrapperFlow
import uk.ac.wellcome.platform.sierra_reader.models.{
  ReaderConfig,
  SierraConfig,
  WindowStatus
}
import uk.ac.wellcome.sierra.{SierraSource, ThrottleRate}
import uk.ac.wellcome.sierra_adapter.services.WindowExtractor
import uk.ac.wellcome.storage.s3.S3Config
import io.circe.syntax._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.platform.sierra_reader.modules.WindowManager

import scala.concurrent.Future
import scala.concurrent.duration._
import uk.ac.wellcome.platform.sierra_reader.sink.SequentialS3Sink

class SierraReaderWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  windowManager: WindowManager,
  s3client: AmazonS3,
  s3Config: S3Config,
  readerConfig: ReaderConfig,
  sierraConfig: SierraConfig
) extends Logging {

  implicit val actorSystem = system
  implicit val materialiser = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  sqsStream.foreach(
    streamName = this.getClass.getSimpleName,
    process = processMessage
  )

  def processMessage(norificationMessage: NotificationMessage): Future[Unit] =
    for {
      messageString <- Future(norificationMessage.Message)
      window <- Future.fromTry(WindowExtractor.extractWindow(messageString))
      windowStatus <- windowManager.getCurrentStatus(window = window)
      _ <- runSierraStream(window = window, windowStatus = windowStatus)
    } yield ()

  private def runSierraStream(
    window: String,
    windowStatus: WindowStatus): Future[PutObjectResult] = {

    info(s"Running the stream with window=$window and status=$windowStatus")

    val baseParams =
      Map("updatedDate" -> window, "fields" -> sierraConfig.fields)
    val params = windowStatus.id match {
      case Some(id) => baseParams ++ Map("id" -> s"[${id.withoutCheckDigit},]")
      case None     => baseParams
    }

    val s3sink = SequentialS3Sink(
      client = s3client,
      bucketName = s3Config.bucketName,
      keyPrefix = windowManager.buildWindowShard(window),
      offset = windowStatus.offset
    )

    val sierraSource = SierraSource(
      apiUrl = sierraConfig.apiUrl,
      oauthKey = sierraConfig.oauthKey,
      oauthSecret = sierraConfig.oauthSec,
      throttleRate = ThrottleRate(3, per = 1.second),
      timeoutMs = 60000)(
      resourceType = sierraConfig.resourceType.toString,
      params)

    val outcome = sierraSource
      .via(SierraRecordWrapperFlow())
      .grouped(readerConfig.batchSize)
      .map(recordBatch => recordBatch.asJson)
      .zipWithIndex
      .runWith(s3sink)

    // This serves as a marker that the window is complete, so we can audit our S3 bucket to see which windows
    // were never successfully completed.
    outcome.map { _ =>
      s3client.putObject(
        s3Config.bucketName,
        s"windows_${sierraConfig.resourceType.toString}_complete/${windowManager
          .buildWindowLabel(window)}",
        "")
    }
  }
}
