package uk.ac.wellcome.platform.sierra_to_dynamo.services

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor.ActorSystem
import com.google.inject.Inject

import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.sierra_to_dynamo.models.AdapterRequest
import uk.ac.wellcome.sierra.{SierraSource, ThrottleRate}
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

class SierraToDynamoWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem
) extends SQSWorker(reader, system, metrics) {

  override def processMessage(message: SQSMessage): Future[Unit] =
    for {
      request <- Future.fromTry(JsonUtil.fromJson[AdapterRequest](message.body))
      source <- SierraSource(
        apiUrl = "https://sierra.example.com/api/",
        oauthKey = oauthKey,
        oauthSecret = oauthSecret,
        throttleRate = ThrottleRate(4, 1.second)
      )(
        resourceType = "items",
        params = request.params
      )
    } yield ()

}
