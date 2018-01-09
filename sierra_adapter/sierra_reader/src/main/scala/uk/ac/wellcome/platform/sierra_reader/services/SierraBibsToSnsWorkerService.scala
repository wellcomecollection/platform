package uk.ac.wellcome.platform.sierra_reader.services

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.sierra_reader.sink.SierraBibsToSnsSink
import uk.ac.wellcome.sierra.{SierraSource, ThrottleRate}
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.sierra_adapter.services.WindowExtractor

import scala.concurrent.Future
import scala.concurrent.duration._

class SierraBibsToSnsWorkerService @Inject()(
  reader: SQSReader,
  writer: SNSWriter,
  system: ActorSystem,
  metrics: MetricsSender,
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
      params = Map("updatedDate" -> window, "fields" -> fields)
      _ <- runSierraStream(params)
    } yield ()

  private def runSierraStream(params: Map[String, String]): Future[Done] = {
    SierraSource(apiUrl, sierraOauthKey, sierraOauthSecret, throttleRate)(
      resourceType = "bibs",
      params).runWith(
      SierraBibsToSnsSink(writer = writer)
    )
  }
}
