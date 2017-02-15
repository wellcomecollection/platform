package uk.ac.wellcome.platform.api.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.xpack.security.XPackElasticClient
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.twitter.inject.TwitterModule
import com.twitter.inject.Logging
import org.elasticsearch.common.settings.Settings


case class XPackConfig(user: String, ssl: Boolean)

object XPackConfigModule extends TwitterModule {
  private val xpackEnabled = flag("es.xpack.enabled", false, "xpack enabled")
  private val user = flag("es.xpack.user", "", "xpack user:password")
  private val sslEnabled = flag("es.xpack.sslEnabled", true, "xpack use ssl")

  @Singleton
  @Provides
  def providesXPackConfig(): Option[XPackConfig] =
    Option(xpackEnabled()).collect { case true =>
      XPackConfig(user(), sslEnabled())
    }
}

object ElasticClientModule extends TwitterModule {
  override val modules = Seq(XPackConfigModule)

  private val host = flag("es.host", "localhost", "host name of ES")
  private val port = flag("es.port", 9300, "port no of ES")
  private val sniff = flag("es.sniff", false, "sniff ES nodes")
  private val clusterName = flag("es.name", "elasticsearch", "cluster name")

  val timeout = flag("es.timeout", 30, "default timeout duration of execution")

  @Singleton
  @Provides
  def provideElasticClient(xpackConfig: Option[XPackConfig]): ElasticClient = {

    val h = host()
    val p = port()

    info(s"Building clientUri for ${h}:${p}")

    val clientUri = ElasticsearchClientUri(h, p)

    val defaultSettings = Settings.builder()
      .put("client.transport.sniff", sniff())
      .put("cluster.name", clusterName())

    val settings = xpackConfig.foldLeft(defaultSettings)((defaults, config) => defaults
      .put("xpack.security.http.ssl.enabled", config.ssl)
      .put("xpack.security.user", config.user)).build()

    XPackElasticClient(settings, clientUri)
  }
}
