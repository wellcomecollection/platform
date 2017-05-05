package uk.ac.wellcome.platform.ingestor

import com.sksamuel.elastic4s.ElasticDsl._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{IdentifiedWork, SourceIdentifier, Work}
import uk.ac.wellcome.platform.ingestor.modules.SQSWorker
import uk.ac.wellcome.test.utils.{ElasticSearchLocal, SQSLocal}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

class IngestorFeatureTest
    extends FunSpec
    with FeatureTestMixin
    with SQSLocal
    with Matchers
    with ElasticSearchLocal with ScalaFutures{

  val ingestorQueueUrl = createQueueAndReturnUrl("test_es_ingestor_queue")

  override val server = new EmbeddedHttpServer(
    new Server() {
      override val modules = Seq(SQSConfigModule,
                                 AkkaModule,
                                 SQSReaderModule,
                                 SQSWorker,
                                 SQSLocalClientModule,
                                 ElasticClientModule)
    },
    flags = Map(
      "aws.region" -> "eu-west-1",
      "aws.sqs.queue.url" -> ingestorQueueUrl,
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
    )
  )

  it("should read an identified unified item from the SQS queue and ingest it into Elasticsearch") {
    val identifiedWork = JsonUtil
      .toJson(
        IdentifiedWork(
          canonicalId = "1234",
          work = Work(
            identifiers = List(SourceIdentifier("Miro", "MiroID", "5678")), label = "some label")))
      .get

    sqsClient.sendMessage(
      ingestorQueueUrl,
      JsonUtil
        .toJson(
          SQSMessage(Some("identified-item"),
                     identifiedWork,
                     "ingester",
                     "messageType",
                     "timestamp"))
        .get
    )

    eventually {
      val hitsFuture = elasticClient.execute(search(s"$index/$itemType").matchAll()).map(_.hits)
      whenReady(hitsFuture) { hits =>
        hits should have size 1
        hits.head.sourceAsString shouldBe identifiedWork
      }
    }
  }
}
