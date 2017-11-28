package uk.ac.wellcome.platform.sierra_to_dynamo.services

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import io.circe.optics.JsonPath.root
import io.circe.parser._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.sierra_to_dynamo.sink.SierraDynamoSink
import uk.ac.wellcome.sierra.{SierraSource, ThrottleRate}
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

class SierraToDynamoWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  dynamoDbClient: AmazonDynamoDB,
  apiUrl: String,
  sierraOauthKey: String,
  sierraOauthSecret: String,
  resourceType: String,
  dynamoTableName: String
) extends SQSWorker(reader, system, metrics) {
  implicit val actorSystem = system
  implicit val materialiser = ActorMaterializer()

  override def processMessage(message: SQSMessage): Future[Unit] =
    for {
      params <- extractUpdatedDateWindow(message)
      _ <- runSierraStream(params)
    } yield ()

  private def runSierraStream(params: Map[String, String]): Future[Done] = {
    SierraSource(apiUrl, sierraOauthKey, sierraOauthSecret, ThrottleRate(30, 1.minute))(
      resourceType,
      params).runWith(SierraDynamoSink(dynamoDbClient, dynamoTableName))
  }

  private def extractUpdatedDateWindow(message: SQSMessage) =
    Future.fromTry(Try(parse(message.body).right.get)).map { json =>
      val start = root.start.string.getOption(json).get
      val end = root.end.string.getOption(json).get

      Map("updatedDate" -> s"[$start,$end]")
    }
}
