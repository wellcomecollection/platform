package uk.ac.wellcome.platform.ingestor.services

import com.sksamuel.elastic4s.http.HttpClient
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.ElasticCredentials
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.{IdentifiedBaseWork, IdentifierType, Subject}
import uk.ac.wellcome.platform.ingestor.config.models.{IngestElasticConfig, IngestorConfig}
import uk.ac.wellcome.platform.ingestor.fixtures.{WorkIndexerFixtures, WorkerServiceFixture}
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class IngestorWorkerServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with Messaging
    with ElasticsearchFixtures
    with SQS
    with S3
    with WorkerServiceFixture
    with WorkIndexerFixtures
    with WorksGenerators
    with CustomElasticsearchMapping {

  it("inserts an Miro identified Work into the index") {
    val miroSourceIdentifier = createSourceIdentifier

    val work = createIdentifiedWorkWith(sourceIdentifier = miroSourceIdentifier)

    assertWorkIsIndexedCorrectly(work)
  }

  it("inserts an Sierra identified Work into the index") {
    val work = createIdentifiedWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier
    )

    assertWorkIsIndexedCorrectly(work)
  }

  it("inserts an Sierra identified invisible Work into the index") {
    val work = createIdentifiedInvisibleWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier
    )

    assertWorkIsIndexedCorrectly(work)
  }

  it("inserts an Sierra identified redirected Work into the index") {
    val work = createIdentifiedRedirectedWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier
    )

    assertWorkIsIndexedCorrectly(work)
  }

  it("inserts a mixture of miro and sierra works into the correct index") {
    val miroWork1 = createIdentifiedWorkWith(
      sourceIdentifier = createMiroSourceIdentifier
    )
    val miroWork2 = createIdentifiedWorkWith(
      sourceIdentifier = createMiroSourceIdentifier
    )
    val sierraWork1 = createIdentifiedWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier
    )
    val sierraWork2 = createIdentifiedWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier
    )

    val works = List(miroWork1, miroWork2, sierraWork1, sierraWork2)

    assertWorksAreIndexedCorrectly(works)
  }

  it("Inserts a non sierra or miro identified work") {
    val work = createIdentifiedWorkWith(
      sourceIdentifier = createSourceIdentifierWith(
        identifierType = IdentifierType("calm-altref-no")
      )
    )

    assertWorkIsIndexedCorrectly(work)
  }

  it(
    "inserts a mixture of miro and sierra works into the index and sends invalid messages to the dlq") {
    val miroWork = createIdentifiedWorkWith(
      sourceIdentifier = createMiroSourceIdentifier
    )
    val sierraWork = createIdentifiedWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier
    )
    val otherWork = createIdentifiedWorkWith(
      sourceIdentifier = createSourceIdentifierWith(
        identifierType = IdentifierType("calm-altref-no")
      )
    )

    val works = List(miroWork, sierraWork, otherWork)

    assertWorksAreIndexedCorrectly(works)
  }

  it(
    "deletes successfully ingested works from the queue, including older versions of already ingested works") {
    val sierraWork = createIdentifiedWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier
    )
    val newSierraWork = createIdentifiedWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier,
      version = 2
    )
    val oldSierraWork = newSierraWork.copy(version = 1)

    val works = List(sierraWork, oldSierraWork)

    withLocalS3Bucket { bucket =>
      withLocalSqsQueueAndDlq {
        case QueuePair(queue, dlq) =>
          withLocalElasticsearchIndex { indexName =>
            withWorkerService(bucket, queue, indexName) { _ =>
              works.foreach { work =>
                sendMessage[IdentifiedBaseWork](
                  bucket = bucket,
                  queue = queue,
                  obj = work)
              }

              assertElasticsearchEventuallyHasWork(
                indexName = indexName,
                sierraWork,
                newSierraWork)
              eventually {
                assertQueueEmpty(queue)
                assertQueueEmpty(dlq)
              }
            }
          }
      }
    }
  }

  it("ingests lots of works") {
    val works = createIdentifiedWorks(count = 250)

    assertWorksAreIndexedCorrectly(works)
  }

  it("only deletes successfully ingested works from the queue") {
    val subsetOfFieldsIndex = new SubsetOfFieldsWorksIndex(
      elasticClient = elasticClient,
      documentType = documentType
    )

    val work = createIdentifiedWork
    val workDoesNotMatchMapping = createIdentifiedWorkWith(
      subjects = List(Subject(label = "crystallography", concepts = Nil))
    )

    val works = List(work, workDoesNotMatchMapping)

    withLocalS3Bucket { bucket =>
      withLocalSqsQueueAndDlq {
        case QueuePair(queue, dlq) =>
          withLocalElasticsearchIndex { indexName =>
            withWorkerService(bucket, queue, indexName) { _ =>
              works.foreach { work =>
                sendMessage[IdentifiedBaseWork](
                  bucket = bucket,
                  queue = queue,
                  obj = work)
              }

              assertElasticsearchNeverHasWork(
                indexName = indexName,
                workDoesNotMatchMapping)
              assertElasticsearchEventuallyHasWork(indexName = indexName, work)
              eventually {
                assertQueueEmpty(queue)
                assertQueueHasSize(dlq, 1)
              }
            }
          }
      }
    }
  }

  it("returns a failed Future if indexing into Elasticsearch fails") {
    withLocalSqsQueueAndDlq {
      case QueuePair(queue, dlq) =>
        withLocalS3Bucket { bucket =>
          val brokenRestClient: RestClient = RestClient
            .builder(new HttpHost("localhost", 9800, "http"))
            .setHttpClientConfigCallback(
              new ElasticCredentials("elastic", "changeme"))
            .build()

          val brokenElasticClient: HttpClient =
            HttpClient.fromRestClient(brokenRestClient)

          withWorkerService(bucket, queue, indexName = "works-v1", elasticClient = brokenElasticClient) { _ =>
            val work = createIdentifiedWork

            sendMessage[IdentifiedBaseWork](
              bucket = bucket,
              queue = queue,
              obj = work)

            eventually {
              assertQueueEmpty(queue)
              assertQueueHasSize(dlq, 1)
            }
          }
        }
    }
  }

  private def withIngestorWorkerService[R](esIndex: String)(
    testWith: TestWith[(QueuePair, Bucket), R]): R =
    withLocalSqsQueueAndDlqAndTimeout(10) {
      case queuePair@QueuePair(queue, dlq) =>
        withLocalS3Bucket { bucket =>
          withWorkerService(bucket, queue, indexName = esIndex) { _ =>
            testWith((queuePair, bucket))
          }
        }

    }

  private def assertWorkIsIndexedCorrectly(work: IdentifiedBaseWork): Seq[Assertion] =
    withLocalElasticsearchIndex { indexName =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { bucket =>
          withWorkerService(bucket, queue, indexName) { _ =>
            sendMessage[IdentifiedBaseWork](
              bucket = bucket,
              queue = queue,
              obj = work)

            assertElasticsearchEventuallyHasWork(indexName = indexName, work)
          }
        }
      }
    }

  private def assertWorksAreIndexedCorrectly(works: Seq[IdentifiedBaseWork]) =
    withLocalS3Bucket { bucket =>
      withLocalSqsQueueAndDlq {
        case QueuePair(queue, dlq) =>
          withLocalElasticsearchIndex { indexName =>
            withWorkerService(bucket, queue, indexName) { _ =>
              works.foreach { work =>
                sendMessage[IdentifiedBaseWork](
                  bucket = bucket,
                  queue = queue,
                  obj = work)
              }

              assertElasticsearchEventuallyHasWork(indexName = indexName, works: _*)
              eventually {
                assertQueueEmpty(queue)
                assertQueueEmpty(dlq)
              }
            }
          }
      }
    }
}
