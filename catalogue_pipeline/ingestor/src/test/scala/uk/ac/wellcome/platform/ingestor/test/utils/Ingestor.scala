package uk.ac.wellcome.platform.ingestor.test.utils

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier, Work}
import uk.ac.wellcome.platform.ingestor.Server
import uk.ac.wellcome.test.utils.{
  AmazonCloudWatchFlag,
  IndexedElasticSearchLocal,
  JsonTestUtil,
  SQSLocal
}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

trait Ingestor
    extends IndexedElasticSearchLocal
    with SQSLocal
    with BeforeAndAfterEach
    with AmazonCloudWatchFlag
    with JsonTestUtil { this: Suite =>

  val queueUrl = createQueueAndReturnUrl("test_es_ingestor_queue")

  val indexName = "works"
  val itemType = "work"

  def createServer: EmbeddedHttpServer = {
    new EmbeddedHttpServer(
      new Server(),
      flags = Map(
        "aws.region" -> "eu-west-1",
        "aws.sqs.queue.url" -> queueUrl,
        "aws.sqs.waitTime" -> "1",
        "es.host" -> "localhost",
        "es.port" -> "9200",
        "es.name" -> "wellcome",
        "es.index" -> indexName,
        "es.type" -> itemType
      ) ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag
    )
  }

  def assertElasticsearchEventuallyHasWork(work: Work) = {
    val workJson = toJson(work).get

    eventually {
      val hits = elasticClient
        .execute(search(s"$indexName/$itemType").matchAllQuery().limit(100))
        .map { _.hits.hits }
        .await

      hits should have size 1

      assertJsonStringsAreEqual(hits.head.sourceAsString, workJson)
    }
  }

  def createWork(canonicalId: String,
                 sourceId: String,
                 title: String,
                 visible: Boolean = true,
                 version: Int = 1): Work = {
    val sourceIdentifier = SourceIdentifier(
      IdentifierSchemes.miroImageNumber,
      sourceId
    )

    Work(title = Some(title),
         sourceIdentifier = sourceIdentifier,
         version = version,
         identifiers = List(sourceIdentifier),
         canonicalId = Some(canonicalId),
         visible = visible)
  }
}
