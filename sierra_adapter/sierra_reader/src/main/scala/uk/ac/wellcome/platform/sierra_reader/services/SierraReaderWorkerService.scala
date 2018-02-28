package uk.ac.wellcome.platform.sierra_reader.services

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectResult
import com.google.inject.Inject
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.sierra_reader.flow.{
  SierraRecordWrapperFlow,
  SierraResourceTypes
}
import uk.ac.wellcome.sierra.{SierraSource, ThrottleRate}
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.sierra_adapter.services.WindowExtractor
import io.circe.syntax._
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.platform.sierra_reader.modules.{
  WindowManager,
  WindowStatus
}

import scala.concurrent.Future
import scala.concurrent.duration._
import uk.ac.wellcome.platform.sierra_reader.sink.SequentialS3Sink

class SierraReaderWorkerService @Inject()(
  reader: SQSReader,
  s3client: AmazonS3,
  system: ActorSystem,
  metrics: MetricsSender,
  windowManager: WindowManager,
  @Flag("reader.batchSize") batchSize: Int,
  resourceType: SierraResourceTypes.Value,
  @Flag("aws.s3.bucketName") bucketName: String,
  @Flag("sierra.apiUrl") apiUrl: String,
  @Flag("sierra.oauthKey") sierraOauthKey: String,
  @Flag("sierra.oauthSecret") sierraOauthSecret: String,
  @Flag("sierra.fields") fields: String
) extends SQSWorker(reader, system, metrics) {

  implicit val actorSystem = system
  implicit val materialiser = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val throttleRate = ThrottleRate(3, 1.second)

  def processMessage(message: SQSMessage): Future[Unit] =
    for {
      window <- Future.fromTry(WindowExtractor.extractWindow(message.body))
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
      bucketName = bucketName,
      keyPrefix = windowManager.buildWindowShard(window),
      offset = windowStatus.offset
    )
    val outcome =
      SierraSource(apiUrl, sierraOauthKey, sierraOauthSecret, throttleRate)(
        resourceType = resourceType.toString,
        params)
        .via(SierraRecordWrapperFlow(resourceType = resourceType))
        .grouped(batchSize)
        .map(recordBatch => recordBatch.asJson)
        .zipWithIndex
        .runWith(s3sink)

    // This serves as a marker that the window is complete, so we can audit our S3 bucket to see which windows
    // were never successfully completed.
    outcome.map { _ =>
      s3client.putObject(
        bucketName,
        s"windows_${resourceType.toString}_complete/${windowManager.buildWindowLabel(window)}",
        "")
    }
  }
}
