package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import com.twitter.inject.TwitterModule
import org.elasticsearch.common.settings.Settings

import org.apache.http.auth.AuthScope
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback

import scala.collection.JavaConverters._

case class XPackConfig(user: String, ssl: Boolean)

object XPackConfigModule extends TwitterModule {
  private val xpackEnabled =
    flag[Boolean]("es.xpack.enabled", false, "xpack enabled")
  private val user = flag[String]("es.xpack.user", "", "xpack user:password")
  private val sslEnabled =
    flag[Boolean]("es.xpack.sslEnabled", true, "xpack use ssl")

  @Singleton
  @Provides
  def providesXPackConfig(): Option[XPackConfig] =
    Option(xpackEnabled()).collect {
      case true =>
        XPackConfig(user(), sslEnabled())
    }
}

class ElasticCredentials extends HttpClientConfigCallback {
  val credentials = new UsernamePasswordCredentials("elastic", "changeme")
  val credentialsProvider = new BasicCredentialsProvider()
  credentialsProvider.setCredentials(AuthScope.ANY, credentials)

  override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
  }
}

object ElasticClientModule extends TwitterModule {
  override val modules = Seq(XPackConfigModule)

  private val host = flag[String]("es.host", "localhost", "host name of ES")
  private val port = flag[Int]("es.port", 9300, "port no of ES")
  private val sniff = flag[Boolean]("es.sniff", false, "sniff ES nodes")
  private val compress =
    flag[Boolean]("es.transport.compress", false, "compress transport")
  private val clusterName =
    flag[String]("es.name", "elasticsearch", "cluster name")

  @Singleton
  @Provides
  def provideElasticClient(xpackConfig: Option[XPackConfig]): HttpClient = {
    info(s"Building clientUri for ${host()}:${port()}")

    // val clientUri = ElasticsearchClientUri(host(), port())

    // val defaultSettings = Settings
    //   .builder()
    //   .put("client.transport.sniff", sniff())
    //   .put("transport.tcp.compress", compress())
    //   .put("cluster.name", clusterName())
    //
    // // val settings = xpackConfig
    // //   .foldLeft(defaultSettings)(
    // //     (defaults, config) =>
    // //       defaults
    // //         .put("xpack.security.transport.ssl.enabled", config.ssl)
    // //         .put("request.headers.X-Found-Cluster", clusterName())
    // //         .put("xpack.security.user", config.user))
    // //   .build()
    //
    // settings.getAsMap.asScala.foreach {
    //   case (k, v) => info(s"ElasticClient setting: $k=$v")
    // }

    val restClient = RestClient
      .builder(new HttpHost(host(), port(), "http"))
      .setHttpClientConfigCallback(new ElasticCredentials())
      .build()

    HttpClient.fromRestClient(restClient)
  }
}
