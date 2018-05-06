package uk.ac.wellcome.platform.ingestor.services

import java.net.ConnectException

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.sksamuel.elastic4s.http.HttpClient
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.finatra.modules.ElasticCredentials
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSReader}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.models.work.internal.{
  IdentifiedWork,
  IdentifierSchemes,
  SourceIdentifier
}
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.test.utils.JsonTestUtil

import scala.concurrent.duration._

class IngestorWorkerServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with MockitoSugar
    with JsonTestUtil
    with ElasticsearchFixtures
    with SQS
    with S3
    with Messaging
    with WorksUtil {

  val itemType = "work"

  val metricsSender: MetricsSender =
    new MetricsSender(
      namespace = "reindexer-tests",
      flushInterval = 100 milliseconds,
      amazonCloudWatch = mock[AmazonCloudWatch],
      actorSystem = ActorSystem())

  val actorSystem = ActorSystem()

  it("inserts an Miro identified Work into v1 and v2 indices") {
    val miroSourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.miroImageNumber,
      ontologyType = "Work",
      value = "M000765"
    )

    val work = createWork().copy(sourceIdentifier = miroSourceIdentifier)

    val workIndexer =
      new WorkIndexer(itemType, elasticClient, metricsSender)

    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withMessageReader[IdentifiedWork, Assertion](bucket, queue) {
          messageReader =>
            withLocalElasticsearchIndex(itemType = itemType) { indexNameV1 =>
              withLocalElasticsearchIndex(itemType = itemType) { indexNameV2 =>
                val service = new IngestorWorkerService(
                  indexNameV1,
                  indexNameV2,
                  identifiedWorkIndexer = workIndexer,
                  messageReader = messageReader,
                  system = actorSystem,
                  metrics = metricsSender
                )

                service.processMessage(work)

                assertElasticsearchEventuallyHasWork(
                  work,
                  indexName = indexNameV1,
                  itemType = itemType)

                assertElasticsearchEventuallyHasWork(
                  work,
                  indexName = indexNameV2,
                  itemType = itemType)
              }
            }
        }
      }
    }
  }

  it("inserts an Sierra identified Work only into the v2 index") {
    val sierraSourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.miroImageNumber,
      ontologyType = "Work",
      value = "b1027467"
    )

    val work = createWork().copy(sourceIdentifier = sierraSourceIdentifier)

    val workIndexer =
      new WorkIndexer(itemType, elasticClient, metricsSender)

    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withMessageReader[IdentifiedWork, Assertion](bucket, queue) {
          messageReader =>
            withLocalElasticsearchIndex(itemType = itemType) { indexNameV1 =>
              withLocalElasticsearchIndex(itemType = itemType) { indexNameV2 =>
                val service = new IngestorWorkerService(
                  indexNameV1,
                  indexNameV2,
                  identifiedWorkIndexer = workIndexer,
                  messageReader = messageReader,
                  system = actorSystem,
                  metrics = metricsSender
                )

                service.processMessage(work)

                assertElasticsearchNeverHasWork(
                  work,
                  indexName = indexNameV1,
                  itemType = itemType)

                assertElasticsearchEventuallyHasWork(
                  work,
                  indexName = indexNameV2,
                  itemType = itemType)

              }
            }
        }
      }
    }
  }

  it("fails inserting a non sierra or miro identified work") {
    val calmSourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.calmAltRefNo,
      ontologyType = "Work",
      value = "MS/237"
    )

    val work = createWork().copy(sourceIdentifier = calmSourceIdentifier)

    val workIndexer =
      new WorkIndexer(itemType, elasticClient, metricsSender)

    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withMessageReader[IdentifiedWork, Assertion](bucket, queue) {
          messageReader =>
            withLocalElasticsearchIndex(itemType = itemType) { indexNameV1 =>
              withLocalElasticsearchIndex(itemType = itemType) { indexNameV2 =>
                val service = new IngestorWorkerService(
                  indexNameV1,
                  indexNameV2,
                  identifiedWorkIndexer = workIndexer,
                  messageReader = messageReader,
                  system = actorSystem,
                  metrics = metricsSender
                )

                val future = service.processMessage(work)

                whenReady(future.failed) { ex =>
                  ex shouldBe a[GracefulFailureException]
                  ex.getMessage shouldBe s"Cannot ingest work with identifierScheme: ${IdentifierSchemes.calmAltRefNo}"

                }
              }
            }
        }
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
      esType = "work",
      elasticClient = brokenElasticClient,
      metricsSender = metricsSender
    )

    val miroSourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.miroImageNumber,
      ontologyType = "Work",
      value = "B000675"
    )

    val work = createWork().copy(sourceIdentifier = miroSourceIdentifier)

    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withMessageReader[IdentifiedWork, Assertion](bucket, queue) {
          messageReader =>
            val service = new IngestorWorkerService(
              "works-v1",
              "works-v2",
              identifiedWorkIndexer = brokenWorkIndexer,
              messageReader = messageReader,
              system = actorSystem,
              metrics = metricsSender
            )

            val future = service.processMessage(work)
            whenReady(future.failed) { result =>
              result shouldBe a[ConnectException]
            }
        }
      }
    }
  }
}
