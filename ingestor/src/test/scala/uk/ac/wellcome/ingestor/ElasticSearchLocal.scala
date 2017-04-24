package uk.ac.wellcome.ingestor

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.xpack.security.XPackElasticClient
import org.elasticsearch.common.settings.Settings
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.time.{Millis, Minute, Second, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, Suite}

trait ElasticSearchLocal
    extends BeforeAndAfterAll
    with Eventually
    with Matchers { this: Suite =>
  private val settings = Settings
    .builder()
    .put("cluster.name", "wellcome")
    .put("xpack.security.user", "elastic:changeme")
    .build()

  var elasticClient =
    XPackElasticClient(settings, ElasticsearchClientUri("localhost", 9300))

  override def beforeAll(): Unit = {
    eventually {

      val client = XPackElasticClient(settings, ElasticsearchClientUri("localhost", 9300))

      if(client.execute(
        clusterHealth()
      ).await.getNumberOfNodes == 1) elasticClient = client
    }

    if (!elasticClient.execute(indexExists("records")).await.isExists)
      elasticClient.execute(createIndex("records")).await
    elasticClient.execute(deleteIn("records").by(matchAllQuery())).await
    super.beforeAll()
  }

  override  def patienceConfig = PatienceConfig(Span(1, Minute), Span(1, Second))
}
