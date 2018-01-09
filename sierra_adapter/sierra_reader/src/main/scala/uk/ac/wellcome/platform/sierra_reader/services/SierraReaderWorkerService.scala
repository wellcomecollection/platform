package uk.ac.wellcome.platform.sierra_reader.services

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.sierra_reader.flow.{SierraRecordWrapperFlow, SierraResourceTypes}
import uk.ac.wellcome.sierra.{SierraSource, ThrottleRate}
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.sierra_adapter.services.WindowExtractor
import io.circe.syntax._
import io.circe.generic.auto._
import uk.ac.wellcome.circe._

import scala.concurrent.Future
import scala.concurrent.duration._
import uk.ac.wellcome.platform.sierra_reader.sink.SequentialS3Sink

class SierraReaderWorkerService @Inject()(
  reader: SQSReader,
  s3client: AmazonS3,
  system: ActorSystem,
  metrics: MetricsSender,
  @Flag("aws.s3.bucketName") bucketName: String,
  @Flag("sierra.apiUrl") apiUrl: String,
  @Flag("sierra.oauthKey") sierraOauthKey: String,
  @Flag("sierra.oauthSecret") sierraOauthSecret: String,
  @Flag("sierra.fields") fields: String
) extends SQSWorker(reader, system, metrics) {

  implicit val actorSystem = system
  implicit val materialiser = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val s3sink = SequentialS3Sink(client = s3client, bucketName = bucketName)

  val throttleRate = ThrottleRate(3, 1.second)

  def processMessage(message: SQSMessage): Future[Unit] =
    for {
      window <- Future.fromTry(WindowExtractor.extractWindow(message.body))
      params = Map("updatedDate" -> window, "fields" -> fields)
      _ <- runSierraStream(params)
    } yield ()

  private def runSierraStream(params: Map[String, String]): Future[Done] = {
    SierraSource(apiUrl, sierraOauthKey, sierraOauthSecret, throttleRate)(resourceType = "bibs", params)
      .via(SierraRecordWrapperFlow(resourceType = SierraResourceTypes.bibs))
      .map(record => record.asJson)
      .zipWithIndex
      .runWith(s3sink)
  }
}
