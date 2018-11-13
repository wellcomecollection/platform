package uk.ac.wellcome.platform.idminter

import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.idminter.config.builders.{IdentifiersTableBuilder, RDSBuilder}
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.models.IdentifiersTable
import uk.ac.wellcome.platform.idminter.services.IdMinterWorkerService
import uk.ac.wellcome.platform.idminter.steps.{IdEmbedder, IdentifierGenerator}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config: Config = ConfigFactory.load()

  val identifierGenerator = new IdentifierGenerator(
    identifiersDao = new IdentifiersDao(
      db = RDSBuilder.buildDB(config),
      identifiers = IdentifiersTableBuilder.buildTable(config)
    )
  )

  val idEmbedder = new IdEmbedder(

  )

  val workerService = new IdMinterWorkerService(

  )

  try {
    info("Starting worker.")

    val result = workerService.run()

    Await.result(result, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info("Terminating worker.")
  }
}
