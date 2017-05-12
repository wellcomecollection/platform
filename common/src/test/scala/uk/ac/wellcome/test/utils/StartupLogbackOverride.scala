package uk.ac.wellcome.test.utils

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import org.slf4j.LoggerFactory

trait StartupLogbackOverride {
  val loggerContext =
    LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
  loggerContext.reset
  val configurator = new JoranConfigurator
  configurator.setContext(loggerContext)
  configurator.doConfigure(
    getClass
      .getResourceAsStream("/logback-startup-test.xml"))
}
