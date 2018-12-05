package uk.ac.wellcome.platform.sierra_reader.services

import java.time.Instant

import akka.Done
import akka.stream.ActorMaterializer
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectResult
import grizzled.slf4j.Logging
import io.circe.Json
import uk.ac.wellcome.messaging.sqs._
import uk.ac.wellcome.platform.sierra_reader.flow.SierraRecordWrapperFlow
import uk.ac.wellcome.platform.sierra_reader.models.{
  SierraResourceTypes,
  WindowStatus
}
import uk.ac.wellcome.sierra.{SierraSource, ThrottleRate}
import uk.ac.wellcome.storage.s3.S3Config
import io.circe.syntax._
import uk.ac.wellcome.Runnable
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.transformable.sierra.{
  AbstractSierraRecord,
  SierraBibRecord,
  SierraItemRecord
}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.sierra_reader.config.models.{
  ReaderConfig,
  SierraAPIConfig
}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import uk.ac.wellcome.platform.sierra_reader.sink.SequentialS3Sink

class SierraReaderWorkerService(
  sqsStream: SQSStream[NotificationMessage],
  s3client: AmazonS3,
  s3Config: S3Config,
  readerConfig: ReaderConfig,
  sierraAPIConfig: SierraAPIConfig
)(implicit ec: ExecutionContext, materializer: ActorMaterializer)
    extends Logging
    with Runnable {

  val windowManager = new WindowManager(
    s3client = s3client,
    s3Config = s3Config,
    readerConfig = readerConfig
  )

  def run(): Future[Done] =
    sqsStream.foreach(
      streamName = this.getClass.getSimpleName,
      process = processMessage
    )

  def processMessage(notificationMessage: NotificationMessage): Future[Unit] =
    for {
      window <- Future.fromTry(
        WindowExtractor.extractWindow(notificationMessage.body)
      )
      windowStatus <- windowManager.getCurrentStatus(window = window)
      _ <- runSierraStream(window = window, windowStatus = windowStatus)
    } yield ()

  private def runSierraStream(
    window: String,
    windowStatus: WindowStatus): Future[PutObjectResult] = {

    info(s"Running the stream with window=$window and status=$windowStatus")

    val baseParams =
      Map("updatedDate" -> window, "fields" -> readerConfig.fields)
    val params = windowStatus.id match {
      case Some(id) => baseParams ++ Map("id" -> s"[$id,]")
      case None     => baseParams
    }

    val s3sink = SequentialS3Sink(
      client = s3client,
      bucketName = s3Config.bucketName,
      keyPrefix = windowManager.buildWindowShard(window),
      offset = windowStatus.offset
    )

    val sierraSource = SierraSource(
      apiUrl = sierraAPIConfig.apiURL,
      oauthKey = sierraAPIConfig.oauthKey,
      oauthSecret = sierraAPIConfig.oauthSec,
      throttleRate = ThrottleRate(3, per = 1.second),
      timeoutMs = 60000
    )(resourceType = readerConfig.resourceType.toString, params)

    val outcome = sierraSource
      .via(SierraRecordWrapperFlow(createRecord))
      .grouped(readerConfig.batchSize)
      .map(recordBatch => toJson(recordBatch))
      .zipWithIndex
      .runWith(s3sink)

    // This serves as a marker that the window is complete, so we can audit
    // our S3 bucket to see which windows were never successfully completed.
    outcome.map { _ =>
      s3client.putObject(
        s3Config.bucketName,
        s"windows_${readerConfig.resourceType.toString}_complete/${windowManager
          .buildWindowLabel(window)}",
        "")
    }
  }

  private def createRecord: (String, String, Instant) => AbstractSierraRecord =
    readerConfig.resourceType match {
      case SierraResourceTypes.bibs  => SierraBibRecord.apply
      case SierraResourceTypes.items => SierraItemRecord.apply
    }

  private def toJson(records: Seq[AbstractSierraRecord]): Json =
    readerConfig.resourceType match {
      case SierraResourceTypes.bibs =>
        records.asInstanceOf[Seq[SierraBibRecord]].asJson
      case SierraResourceTypes.items =>
        records.asInstanceOf[Seq[SierraItemRecord]].asJson
    }
}
