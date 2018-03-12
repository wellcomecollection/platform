package uk.ac.wellcome.platform.ingestor.services

import java.net.ConnectException

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.sksamuel.elastic4s.http.HttpClient
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SQSConfig, SQSMessage}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.finatra.modules.ElasticCredentials
import uk.ac.wellcome.models.{
  IdentifiedWork,
  IdentifierSchemes,
  SourceIdentifier
}
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.test.fixtures.{ElasticsearchFixtures, SqsFixtures}
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
    with ElasticsearchFixtures
    with SqsFixtures {

  val indexName = "works"
  val itemType = "work"

  val metricsSender: MetricsSender =
    new MetricsSender(
      namespace = "reindexer-tests",
      100 milliseconds,
      mock[AmazonCloudWatch],
      ActorSystem())

  val workIndexer =
    new WorkIndexer(indexName, itemType, elasticClient, metricsSender)
  val actorSystem = ActorSystem()

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

  it("should insert an identified Work into Elasticsearch") {
    val work = createWork(
      canonicalId = "m7b2aqtw",
      sourceId = "M000765",
      title = "A monstrous monolith of moss"
    )

    val sqsMessage = messageFromString(toJson(work).get)

    withLocalSqsQueue { queueUrl =>
      withLocalElasticsearchIndex(indexName = indexName, itemType = itemType) {
        _ =>
          val service = new IngestorWorkerService(
            identifiedWorkIndexer = workIndexer,
            reader = new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
            system = actorSystem,
            metrics = metricsSender
          )

          service.processMessage(sqsMessage)

          eventually {
            assertElasticsearchEventuallyHasWork(
              work,
              indexName = indexName,
              itemType = itemType)
          }
      }
    }
  }

  it("should return a failed future if the input string is not a Work") {
    val sqsMessage = messageFromString("<xml><item> ??? Not JSON!!")

    withLocalSqsQueue { queueUrl =>
      val workIndexer = new WorkIndexer(
        esIndex = indexName,
        esType = itemType,
        elasticClient = elasticClient,
        metricsSender = metricsSender
      )

      val service = new IngestorWorkerService(
        identifiedWorkIndexer = workIndexer,
        reader = new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
        system = actorSystem,
        metrics = metricsSender
      )

      val future = service.processMessage(sqsMessage)

      whenReady(future.failed) { exception =>
        exception shouldBe a[GracefulFailureException]
      }
    }
  }

  it("returns a failed Future if indexing into Elasticsearch fails") {
    val brokenRestClient: RestClient = RestClient
      .builder(new HttpHost("localhost", 9800, "http"))
      .setHttpClientConfigCallback(
        new ElasticCredentials("elastic", "changeme"))
      .build()

    val brokenElasticClient: HttpClient =
      HttpClient.fromRestClient(brokenRestClient)

    val brokenWorkIndexer = new WorkIndexer(
      esIndex = "works",
      esType = "work",
      elasticClient = brokenElasticClient,
      metricsSender = metricsSender
    )

    val work = createWork(
      canonicalId = "b4aurznb",
      sourceId = "B000765",
      title = "A broken beach of basilisks"
    )

    val sqsMessage = messageFromString(toJson(work).get)

    withLocalSqsQueue { queueUrl =>
      val service = new IngestorWorkerService(
        identifiedWorkIndexer = brokenWorkIndexer,
        reader = new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
        system = actorSystem,
        metrics = metricsSender
      )

      val future = service.processMessage(sqsMessage)
      whenReady(future.failed) { result =>
        result shouldBe a[ConnectException]
      }
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
}
