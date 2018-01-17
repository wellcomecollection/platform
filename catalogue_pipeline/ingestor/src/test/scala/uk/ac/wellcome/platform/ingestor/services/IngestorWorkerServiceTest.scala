package uk.ac.wellcome.platform.ingestor.services

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SQSConfig, SQSMessage}
import uk.ac.wellcome.platform.ingestor.test.utils.Ingestor
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.duration._

class IngestorWorkerServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with MockitoSugar
    with JsonTestUtil
    with Ingestor {

  val metricsSender: MetricsSender =
    new MetricsSender(namespace = "reindexer-tests", mock[AmazonCloudWatch])

  val workIndexer =
    new WorkIndexer(indexName, itemType, elasticClient, metricsSender)
  val actorSystem = ActorSystem()

  val service = new IngestorWorkerService(
    identifiedWorkIndexer = workIndexer,
    reader = new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
    system = actorSystem,
    metrics = metricsSender
  )

  it("should insert an identified Work into Elasticsearch") {
    val work = createWork(
      canonicalId = "m7b2aqtw",
      sourceId = "M000765",
      title = "A monstrous monolith of moss"
    )

    val sqsMessage = messageFromString(toJson(work).get)
    service.processMessage(sqsMessage)

    eventually {
      assertElasticsearchEventuallyHasWork(work)
    }
  }

  it("should return a failed future if the input string is not a Work") {
    val sqsMessage = messageFromString("<xml><item> ??? Not JSON!!")
    val future = service.processMessage(sqsMessage)

    whenReady(future.failed) { exception =>
      exception shouldBe a[GracefulFailureException]
    }
  }

  it(
    "should not return a NullPointerException if the document is the string null") {
    val sqsMessage = messageFromString("<xml><item> ??? Not JSON!!")
    val future = service.processMessage(sqsMessage)

    whenReady(future.failed) { exception =>
      exception shouldBe a[GracefulFailureException]
    }
  }

  private def messageFromString(body: String): SQSMessage =
    SQSMessage(
      subject = Some("inserting-identified-work-test"),
      body = body,
      topic = "arn:aws:topic:123",
      messageType = "json",
      timestamp = "2018-01-16T15:24:00Z"
    )

  private def assertElasticsearchEventuallyHasWork(work: Work) = {
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

  private def createWork(canonicalId: String, sourceId: String, title: String): Work = {
    val sourceIdentifier = SourceIdentifier(
      IdentifierSchemes.miroImageNumber,
      sourceId
    )

    Work(
      canonicalId = Some(canonicalId),
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier),
      title = title
    )
  }
}
