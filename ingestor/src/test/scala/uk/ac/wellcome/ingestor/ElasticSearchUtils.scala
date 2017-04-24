package uk.ac.wellcome.ingestor

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.xpack.security.XPackElasticClient
import org.elasticsearch.common.settings.Settings
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterEach, Matchers, Suite}

trait ElasticSearchUtils extends BeforeAndAfterEach with Eventually with IntegrationPatience with Matchers { this: Suite =>
  private val settings = Settings
    .builder()
    .put("cluster.name", "wellcome")
    .put("xpack.security.user", "elastic:changeme")
    .build()

  val elasticClient =
    XPackElasticClient(settings, ElasticsearchClientUri("localhost", 9300))

  override def beforeEach(): Unit = {
    eventually {
      elasticClient.execute(
        clusterHealth()
      ).await.getNumberOfNodes shouldBe 1
    }

    if (!elasticClient.execute(indexExists("records")).await.isExists)
      elasticClient.execute(createIndex("records")).await
    elasticClient.execute(deleteIn("records").by(matchAllQuery())).await
    super.beforeEach()
  }
}
