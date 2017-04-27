package uk.ac.wellcome.platform.ingestor

import com.sksamuel.elastic4s.ElasticDsl._
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, IntegrationTest}
import org.scalatest.Matchers
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{IdentifiedUnifiedItem, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.platform.ingestor.modules.SQSWorker
import uk.ac.wellcome.test.utils.{ElasticSearchLocal, SQSLocal}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

class IngestorIntegrationTest
    extends IntegrationTest
    with SQSLocal
    with Matchers
    with ElasticSearchLocal {

  override def queueName: String = "test_es_ingestor_queue"
  val itemType = "item"
  override def injector: Injector = {
    TestInjector(
      flags = Map(
        "aws.region" -> "eu-west-1",
        "aws.sqs.queue.url" -> queueUrl,
        "aws.sqs.waitTime" -> "1",
        "es.host" -> "localhost",
        "es.port" -> "9300",
        "es.name" -> "wellcome",
        "es.xpack.enabled" -> "true",
        "es.xpack.user" -> "elastic:changeme",
        "es.xpack.sslEnabled" -> "false",
        "es.sniff" -> "false",
        "es.index" -> index,
        "es.type" -> itemType
      ),
      modules = Seq(SQSConfigModule,
        AkkaModule,
        SQSReaderModule,
        SQSWorker,
        SQSLocalClientModule,
        ElasticClientModule)
    )
  }

  test("it should read an identified unified item from the SQS queue and ingest it into Elasticsearch") {
    val identifiedUnifiedItem = JsonUtil
      .toJson(
        IdentifiedUnifiedItem(
          canonicalId = "1234",
          unifiedItem = UnifiedItem(
            identifiers = List(SourceIdentifier("Miro", "MiroID", "5678")), label = "some label")))
      .get

    sqsClient.sendMessage(
      queueUrl,
      JsonUtil
        .toJson(
          SQSMessage(Some("identified-item"),
                     identifiedUnifiedItem,
                     "ingester",
                     "messageType",
                     "timestamp"))
        .get
    )

    SQSWorker.singletonStartup(injector)

    eventually {
      val hits =
        elasticClient
          .execute(search(s"$index/$itemType").matchAll())
          .map(_.hits)
          .await
      hits should have size 1
      hits.head.sourceAsString shouldBe identifiedUnifiedItem
    }
  }
}
