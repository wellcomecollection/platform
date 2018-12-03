package uk.ac.wellcome.config.core

import com.typesafe.config.{Config, ConfigFactory}
import uk.ac.wellcome.{WellcomeApp, WorkerService}

trait WellcomeTypesafeApp extends WellcomeApp {
  def runWithConfig(builder: Config => WorkerService) = {
    val config: Config = ConfigFactory.load()
    val workerService = builder(config)
    run(workerService)
  }
}
