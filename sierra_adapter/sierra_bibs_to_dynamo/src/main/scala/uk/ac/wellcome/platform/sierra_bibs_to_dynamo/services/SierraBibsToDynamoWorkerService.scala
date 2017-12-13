package uk.ac.wellcome.platform.sierra_bibs_to_dynamo.services

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{DynamoConfig, SQSMessage}
import uk.ac.wellcome.platform.sierra_bibs_to_dynamo.sink.SierraBibsDynamoSink
import uk.ac.wellcome.sierra.{SierraSource, ThrottleRate}
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.sierra_adapter.services.WindowExtractor

import scala.concurrent.Future
import scala.concurrent.duration._

class SierraBibsToDynamoWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  dynamoDbClient: AmazonDynamoDB,
  @Flag("sierra.apiUrl") apiUrl: String,
  @Flag("sierra.oauthKey") sierraOauthKey: String,
  @Flag("sierra.oauthSecret") sierraOauthSecret: String,
  @Flag("sierra.fields") fields: String,
  dynamoConfig: DynamoConfig
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
      SierraBibsDynamoSink(
        client = dynamoDbClient,
        tableName = dynamoConfig.table
      ))
  }
}
