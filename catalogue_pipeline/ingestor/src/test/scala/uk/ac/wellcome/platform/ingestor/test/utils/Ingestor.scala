package uk.ac.wellcome.platform.ingestor.test.utils

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.ac.wellcome.models.{
  IdentifiedWork,
  IdentifierSchemes,
  SourceIdentifier
}
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

  def createWork(canonicalId: String,
                 sourceId: String,
                 title: String,
                 visible: Boolean = true,
                 version: Int = 1): IdentifiedWork = {
    val sourceIdentifier = SourceIdentifier(
      IdentifierSchemes.miroImageNumber,
      sourceId
    )

    IdentifiedWork(
      title = Some(title),
      sourceIdentifier = sourceIdentifier,
      version = version,
      identifiers = List(sourceIdentifier),
      canonicalId = canonicalId,
      visible = visible)
  }
}
