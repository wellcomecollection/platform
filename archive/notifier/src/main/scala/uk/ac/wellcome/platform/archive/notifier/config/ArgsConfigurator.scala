package uk.ac.wellcome.platform.archive.notifier.config

import java.net.URL

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.platform.archive.notifier.models.NotifierConfig

class ArgsConfigurator(val arguments: Seq[String])
    extends ScallopConf(arguments) {

  private val contextUrl =
    opt[URL]("context-url", required = true)

  verify()

  val appConfig = NotifierConfig(
    contextUrl = contextUrl()
  )
}
