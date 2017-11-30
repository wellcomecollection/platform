package uk.ac.wellcome.platform.sierra_to_dynamo.services

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.twitter.inject.annotations.Flag
import io.circe.optics.JsonPath.root
import io.circe.parser._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{DynamoConfig, SQSMessage}
import uk.ac.wellcome.platform.sierra_to_dynamo.sink.SierraDynamoSink
import uk.ac.wellcome.sierra.{SierraSource, ThrottleRate}
import uk.ac.wellcome.sqs.{SQSReader, SQSReaderGracefulException, SQSWorker}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

class SierraToDynamoWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  dynamoDbClient: AmazonDynamoDB,
  @Flag("sierra.apiUrl") apiUrl: String,
  @Flag("sierra.oauthKey") sierraOauthKey: String,
  @Flag("sierra.oauthSecret") sierraOauthSecret: String,
  @Flag("sierra.resourceType") resourceType: String,
  dynamoConfig: DynamoConfig
) extends SQSWorker(reader, system, metrics) {

  implicit val actorSystem = system
  implicit val materialiser = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val throttleRate = ThrottleRate(3, 1.second)

  def processMessage(message: SQSMessage): Future[Unit] =
    for {
      window <- Future.fromTry(WindowExtractor.extractWindow(message.body))
      params = Map("updatedDate" -> window)
      _ <- runSierraStream(params)
    } yield ()

  private def runSierraStream(params: Map[String, String]): Future[Done] = {
    SierraSource(apiUrl, sierraOauthKey, sierraOauthSecret, throttleRate)(
      resourceType,
      params).runWith(SierraDynamoSink(dynamoDbClient, dynamoConfig.table))
  }
}
