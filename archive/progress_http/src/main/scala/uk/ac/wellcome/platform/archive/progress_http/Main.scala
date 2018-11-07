package uk.ac.wellcome.platform.archive.progress_http

import com.typesafe.config.ConfigFactory
import uk.ac.wellcome.platform.archive.common.config.builders.{DynamoBuilder, HTTPServerBuilder, SNSBuilder}
import uk.ac.wellcome.platform.archive.progress_http.modules._

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App with AkkaHttpApp {
  val config = ConfigFactory.load()

  val progressHTTP = new ProgressHTTP(
    dynamoClient = DynamoBuilder.buildDynamoClient(config),
    dynamoConfig = DynamoBuilder.buildDynamoConfig(config),
    snsWriter = SNSBuilder.buildSNSWriter(config),
    httpServerConfig = HTTPServerBuilder.buildHTTPServerConfig(config),
    contextURL = HTTPServerBuilder.buildContextURL(config)
  )

  try {
    info(s"Starting service.")

    val app = progressHTTP.run()

    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating service.")
  }
}
