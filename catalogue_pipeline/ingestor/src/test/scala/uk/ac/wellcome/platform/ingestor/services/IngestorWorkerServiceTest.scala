package uk.ac.wellcome.platform.ingestor.services

import java.net.ConnectException

import com.sksamuel.elastic4s.http.HttpClient
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.finatra.modules.ElasticCredentials
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSReader}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.models.work.internal.{
  IdentifiedWork,
  IdentifierSchemes,
  SourceIdentifier
}
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.platform.ingestor.fixtures.WorkIndexerFixture
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.JsonTestUtil

import scala.concurrent.duration._

class IngestorWorkerServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with JsonTestUtil
    with ElasticsearchFixtures
    with SQS
    with S3
    with WorkIndexerFixture
    with Messaging
    with WorksUtil {

  val esType = "work"

  it("inserts an Miro identified Work into v1 and v2 indices") {
    val miroSourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.miroImageNumber,
      ontologyType = "Work",
      value = "M000765"
    )

    val work = createWork().copy(sourceIdentifier = miroSourceIdentifier)

    withLocalElasticsearchIndex(esType = esType) { esIndexV1 =>
      withLocalElasticsearchIndex(esType = esType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) { service =>
          service.processMessage(work)

          assertElasticsearchEventuallyHasWork(
            work,
            indexName = indexNameV1,
            esType = esType)

          assertElasticsearchEventuallyHasWork(
            work,
            indexName = indexNameV2,
            esType = esType)
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

    withLocalElasticsearchIndex(esType = esType) { esIndexV1 =>
      withLocalElasticsearchIndex(esType = esType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) { service =>
          service.processMessage(work)

          assertElasticsearchNeverHasWork(
            work,
            indexName = indexNameV1,
            esType = esType)

          assertElasticsearchEventuallyHasWork(
            work,
            indexName = indexNameV2,
            esType = esType)
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

    withLocalElasticsearchIndex(esType = esType) { esIndexV1 =>
      withLocalElasticsearchIndex(esType = esType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) { service =>
          val future = service.processMessage(work)

          whenReady(future.failed) { ex =>
            ex shouldBe a[GracefulFailureException]
            ex.getMessage shouldBe s"Cannot ingest work with identifierScheme: ${IdentifierSchemes.calmAltRefNo}"
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

    withWorkIndexer(esType, elasticClient = brokenElasticClient) { workIndexer =>
      withIngestorWorkerService("works-v1", "works-v2", workIndexer) { service =>
        val future = service.processMessage(work)
        whenReady(future.failed) { result =>
          result shouldBe a[ConnectException]
        }
      }
    }
  }

  private def withIngestorWorkerService[R](
    esIndexV1: String,
    esIndexV2: String)(testWith: TestWith[IngestorWorkerService, R]): R = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withWorkIndexer(esType, elasticClient, metricsSender) { workIndexer =>
          withIngestorWorkerService(esIndexV1, esIndexV2, actorSystem, metricsSender, workIndexer) { service =>
            testWith(service)
          }
        }
      }
    }
  }

  private def withIngestorWorkerService[R](
    esIndexV1: String,
    esIndexV2: String,
    actorSystem: ActorSystem,
    metricsSender: MetricsSender,
    workIndexer: WorkIndexer)(testWith: TestWith[IngestorWorkerService, R]): R = {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withMessageReader[IdentifiedWork, Assertion](bucket, queue) { messageReader =>
          withWorkIndexer(esType, elasticClient, metricsSender) { workIndexer =>
            val service = new IngestorWorkerService(
              esIndexV1 = esIndexV1,
              esIndexV2 = esIndexV2,
              workIndexer = workIndexer,
              messageReader = messageReader,
              system = actorSystem,
              metrics = metricsSender
            )

            testWith(service)
          }
        }
      }
    }
  }
}
