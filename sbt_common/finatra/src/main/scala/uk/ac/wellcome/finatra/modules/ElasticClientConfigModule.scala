package uk.ac.wellcome.finatra.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.elasticsearch.ElasticClientConfig

// Move this to finatra-common
object ElasticClientConfigModule extends TwitterModule {

  // config goes in f-common
  private val host = flag[String]("es.host", "localhost", "host name of ES")
  private val port = flag[Int]("es.port", 9200, "port no of ES")
  private val protocol =
    flag[String]("es.protocol", "http", "protocol for talking to ES")
  private val clusterName =
    flag[String]("es.name", "elasticsearch", "cluster name")
  private val username = flag[String]("es.username", "elastic", "ES username")
  private val password = flag[String]("es.password", "changeme", "ES username")

  @Singleton
  @Provides
  def provideElasticClientConfig(): ElasticClientConfig = {
    ElasticClientConfig(host(), port(), protocol(), username(), password())
  }
}
