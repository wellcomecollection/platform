package uk.ac.wellcome.platform.sierra_reader.services

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectResult
import com.google.inject.Inject
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import io.circe.syntax._
import uk.ac.wellcome.messaging.sqs._
import uk.ac.wellcome.platform.sierra_reader.flow.SierraRecordWrapperFlow
import uk.ac.wellcome.platform.sierra_reader.models.SierraResourceTypes
import uk.ac.wellcome.platform.sierra_reader.modules.{WindowManager, WindowStatus}
import uk.ac.wellcome.platform.sierra_reader.sink.SequentialS3Sink
import uk.ac.wellcome.sierra.{SierraSource, ThrottleRate}
import uk.ac.wellcome.sierra_adapter.services.WindowExtractor
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.concurrent.duration._

class SierraReaderWorkerService @Inject()(
  sqsStream: SQSStream[String],
  windowManager: WindowManager,
  s3client: AmazonS3,
  s3Config: S3Config,
  @Flag("reader.batchSize") batchSize: Int,
  resourceType: SierraResourceTypes.Value,
  @Flag("sierra.apiUrl") apiUrl: String,
  @Flag("sierra.oauthKey") sierraOauthKey: String,
  @Flag("sierra.oauthSecret") sierraOauthSecret: String,
  @Flag("sierra.fields") fields: String
) extends Logging {

  // This is the throttle rate for requests to Sierra, *not* the rate at
  // which we fetch messages from SQS.
  val throttleRate = ThrottleRate(3, 1.second)

  sqsStream.foreach(
    streamName = this.getClass.getSimpleName,
    process = processMessage
  )

  def processMessage(messageString: String): Future[Unit] =
    for {
      window <- Future.fromTry(WindowExtractor.extractWindow(messageString))
      windowStatus <- windowManager.getCurrentStatus(window = window)
      _ <- runSierraStream(window = window, windowStatus = windowStatus)
    } yield ()

  private def runSierraStream(
    window: String,
    windowStatus: WindowStatus): Future[PutObjectResult] = {

    info(s"Running the stream with window=$window and status=$windowStatus")

    val baseParams = Map("updatedDate" -> window, "fields" -> fields)
    val params = windowStatus.id match {
      case Some(id) => baseParams ++ Map("id" -> s"[$id,]")
      case None => baseParams
    }

    val s3sink = SequentialS3Sink(
      client = s3client,
      bucketName = s3Config.bucketName,
      keyPrefix = windowManager.buildWindowShard(window),
      offset = windowStatus.offset
    )

    val outcome =
      SierraSource(apiUrl, sierraOauthKey, sierraOauthSecret, throttleRate)(
        resourceType = resourceType.toString,
        params)
        .via(SierraRecordWrapperFlow())
        .grouped(batchSize)
        .map(recordBatch => recordBatch.asJson)
        .zipWithIndex
        .runWith(s3sink)

    // This serves as a marker that the window is complete, so we can audit our S3 bucket to see which windows
    // were never successfully completed.
    outcome.map { _ =>
      s3client.putObject(
        s3Config.bucketName,
        s"windows_${resourceType.toString}_complete/${windowManager.buildWindowLabel(window)}",
        "")
    }
  }
}
