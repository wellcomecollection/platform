package uk.ac.wellcome.platform.matcher

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.{MessagingBuilder, SNSBuilder}
import uk.ac.wellcome.config.monitoring.builders.MetricsBuilder
import uk.ac.wellcome.config.storage.builders.DynamoBuilder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.matcher.locking.{
  DynamoLockingService,
  DynamoRowLockDao
}
import uk.ac.wellcome.platform.matcher.matcher.WorkMatcher
import uk.ac.wellcome.platform.matcher.services.MatcherWorkerService
import uk.ac.wellcome.platform.matcher.storage.{WorkGraphStore, WorkNodeDao}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem =
    AkkaBuilder.buildActorSystem()
  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val dynamoClient = DynamoBuilder.buildDynamoClient(config)

  val workGraphStore = new WorkGraphStore(
    workNodeDao = new WorkNodeDao(
      dynamoDbClient = dynamoClient,
      dynamoConfig = DynamoBuilder.buildDynamoConfig(config)
    )
  )

  val lockingService = new DynamoLockingService(
    dynamoRowLockDao = new DynamoRowLockDao(
      dynamoDBClient = dynamoClient,
      dynamoConfig =
        DynamoBuilder.buildDynamoConfig(config, namespace = "locking.service")
    ),
    metricsSender = MetricsBuilder.buildMetricsSender(config)
  )

  val workMatcher = new WorkMatcher(
    workGraphStore = workGraphStore,
    lockingService = lockingService
  )

  val workerService = new MatcherWorkerService(
    messageStream =
      MessagingBuilder.buildMessageStream[TransformedBaseWork](config),
    snsWriter = SNSBuilder.buildSNSWriter(config),
    workMatcher = workMatcher
  )

  try {
    info(s"Starting worker.")

    val result = workerService.run()

    Await.result(result, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }
}
