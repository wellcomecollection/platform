package uk.ac.wellcome.ingestor

import com.sksamuel.elastic4s.testkit.ElasticSugar
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, IntegrationTest}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{
  IdentifiedUnifiedItem,
  SourceIdentifier,
  UnifiedItem
}
import uk.ac.wellcome.platform.ingestor.modules.SQSWorker
import uk.ac.wellcome.test.utils.SQSLocal
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.GlobalExecutionContext.context

class IngestorIntegrationTest
    extends IntegrationTest
    with SQSLocal
    with ElasticSugar
    with Eventually
    with IntegrationPatience {

  override def injector: Injector =
    TestInjector(
      flags = Map(
        "aws.region" -> "eu-west-1",
        "aws.sqs.queue.url" -> ingesterQueueUrl,
        "aws.sqs.waitTime" -> "1",
        "es.host" -> "localhost",
        "es.port" -> 9200.toString,
        "es.index" -> "records",
        "es.type" -> "item"
      ),
      modules = Seq(SQSConfigModule,
        AkkaModule,
        SQSReaderModule,
        SQSWorker,
        SQSLocalClientModule,
        ElasticClientModule)
    )

  test("it should read a unified item from the SQS queue and ingest it into elastic search") {
    val identifiedUnifiedItem = JsonUtil
      .toJson(
        IdentifiedUnifiedItem(
          canonicalId = "1234",
          unifiedItem = UnifiedItem(
            identifiers = List(SourceIdentifier("Miro", "MiroID", "5678")))))
      .get

    sqsClient.sendMessage(
      ingesterQueueUrl,
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
        client.execute(search("records/item").matchAll()).map(_.hits).await
      hits should have size 1
      hits.head.sourceAsString shouldBe identifiedUnifiedItem
    }
  }

}
