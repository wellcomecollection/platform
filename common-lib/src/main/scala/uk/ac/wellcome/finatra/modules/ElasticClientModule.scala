package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.xpack.security.XPackElasticClient
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.twitter.inject.TwitterModule
import com.twitter.inject.Logging
import org.elasticsearch.common.settings.Settings
import scala.collection.JavaConverters._


case class XPackConfig(user: String, ssl: Boolean)

object XPackConfigModule extends TwitterModule {
  private val xpackEnabled = flag[Boolean]("es.xpack.enabled", false, "xpack enabled")
  private val user = flag[String]("es.xpack.user", "", "xpack user:password")
  private val sslEnabled = flag[Boolean]("es.xpack.sslEnabled", true, "xpack use ssl")

  @Singleton
  @Provides
  def providesXPackConfig(): Option[XPackConfig] =
    Option(xpackEnabled()).collect { case true =>
      XPackConfig(user(), sslEnabled())
    }
}

object ElasticClientModule extends TwitterModule {
  override val modules = Seq(XPackConfigModule)

  private val host = flag[String]("es.host", "localhost", "host name of ES")
  private val port = flag[Int]("es.port", 9300, "port no of ES")
  private val sniff = flag[Boolean]("es.sniff", false, "sniff ES nodes")
  private val compress = flag[Boolean]("es.transport.compress", false, "compress transport")
  private val clusterName = flag[String]("es.name", "elasticsearch", "cluster name")

  val timeout = flag("es.timeout", 30, "default timeout duration of execution")

  @Singleton
  @Provides
  def provideElasticClient(xpackConfig: Option[XPackConfig]): ElasticClient = {
    info(s"Building clientUri for ${host()}:${port()}")

    val clientUri = ElasticsearchClientUri(host(), port())

    val defaultSettings = Settings.builder()
      .put("client.transport.sniff", sniff())
      .put("transport.tcp.compress", compress())
      .put("cluster.name", clusterName())

    val settings = xpackConfig.foldLeft(defaultSettings)((defaults, config) => defaults
      .put("xpack.security.transport.ssl.enabled", config.ssl)
      .put("request.headers.X-Found-Cluster", "${cluster.name}")
      .put("xpack.security.user", config.user)).build()

    settings.getAsMap.asScala.map {
      case (k,v) => info(s"ElasticClient setting: ${k}=${v}")
    }

    XPackElasticClient(settings, clientUri)
  }
}
