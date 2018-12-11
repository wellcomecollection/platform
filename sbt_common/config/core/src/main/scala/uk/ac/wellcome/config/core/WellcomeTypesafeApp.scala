package uk.ac.wellcome.config.core

import com.typesafe.config.{Config, ConfigFactory}
import uk.ac.wellcome.{Runnable, WellcomeApp}

trait WellcomeTypesafeApp extends WellcomeApp {
  def runWithConfig(builder: Config => Runnable) = {
    val config: Config = ConfigFactory.load()
    val workerService = builder(config)
    run(workerService)
  }
}