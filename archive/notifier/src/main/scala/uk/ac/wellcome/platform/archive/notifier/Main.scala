package uk.ac.wellcome.platform.archive.notifier

import java.net.URL

import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.config.builders.EnrichConfig._
import uk.ac.wellcome.platform.archive.common.config.builders.{MetricsBuilder, SNSBuilder, SQSBuilder}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config = ConfigFactory.load()

  val notifier = new Notifier(
    sqsClient = SQSBuilder.buildSQSAsyncClient(config),
    sqsConfig = SQSBuilder.buildSQSConfig(config),
    snsClient = SNSBuilder.buildSNSClient(config),
    snsConfig = SNSBuilder.buildSNSConfig(config),
    metricsSender = MetricsBuilder.buildMetricsSender(config),
    contextUrl = buildContextURL(config)
  )

  try {
    info(s"Starting worker.")

    val result = notifier.run()

    Await.result(result, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }

  private def buildContextURL(config: Config): URL =
    new URL(config.required[String]("notifier.context-url"))
}
