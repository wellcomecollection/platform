package uk.ac.wellcome.platform.archive.common.modules

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config

object HttpServerConfigModule extends AbstractModule {
  import uk.ac.wellcome.platform.archive.common.models.EnrichConfig._

  @Provides
  @Singleton
  def providesHttpServerConfig(config: Config) = {
    val host = config
      .get[String]("http.server.host")
      .getOrElse("0.0.0.0")

    val port = config
      .get[Int]("http.server.host")
      .getOrElse(9001)

    HttpServerConfig(host, port)
  }

}

case class HttpServerConfig(
  host: String,
  port: Int
)
