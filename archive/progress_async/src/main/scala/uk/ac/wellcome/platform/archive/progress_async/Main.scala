package uk.ac.wellcome.platform.archive.progress_async

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.config.builders._
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

object Main extends App with Logging {
  val config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  val progressAsync = new ProgressAsync(
    messageStream = MessagingBuilder.buildMessageStream[NotificationMessage, Unit](config),
    progressTracker = new ProgressTracker(
      dynamoClient = DynamoBuilder.buildDynamoClient(config),
      dynamoConfig = DynamoBuilder.buildDynamoConfig(config)
    ),
    snsClient = SNSBuilder.buildSNSClient(config),
    snsConfig = SNSBuilder.buildSNSConfig(config)
  )

  try {
    info(s"Starting worker.")

    val app = progressAsync.run()

    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }
}
