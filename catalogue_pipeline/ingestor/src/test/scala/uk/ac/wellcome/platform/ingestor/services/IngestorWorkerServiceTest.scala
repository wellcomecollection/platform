package uk.ac.wellcome.platform.ingestor.services

import akka.actor.ActorSystem
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
import uk.ac.wellcome.models.work.internal.{
  IdentifiedBaseWork,
  IdentifierType,
  Subject
}
import uk.ac.wellcome.platform.ingestor.{IngestElasticConfig, IngestorConfig}
import uk.ac.wellcome.platform.ingestor.fixtures.WorkIndexerFixtures
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

class IngestorWorkerServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with Messaging
    with ElasticsearchFixtures
    with SQS
    with S3
    with WorkIndexerFixtures
    with WorksGenerators
    with CustomElasticsearchMapping {

  val itemType = "work"

  it("inserts an Miro identified Work into the index") {
    val miroSourceIdentifier = createSourceIdentifier

    val work = createIdentifiedWorkWith(sourceIdentifier = miroSourceIdentifier)

    withLocalElasticsearchIndex(itemType = itemType) { esIndex =>
      withIngestorWorkerService(esIndex) {
        case (QueuePair(queue, _), bucket) =>
          sendMessage[IdentifiedBaseWork](
            bucket = bucket,
            queue = queue,
            obj = work)

          assertElasticsearchEventuallyHasWork(
            indexName = esIndex,
            itemType = itemType,
            work)
      }
    }
  }

  it("inserts an Sierra identified Work into the index") {
    val work = createIdentifiedWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier
    )

    withLocalElasticsearchIndex(itemType = itemType) { esIndex =>
      withIngestorWorkerService(esIndex) {
        case (QueuePair(queue, _), bucket) =>
          sendMessage[IdentifiedBaseWork](
            bucket = bucket,
            queue = queue,
            obj = work)

          assertElasticsearchEventuallyHasWork(
            indexName = esIndex,
            itemType = itemType,
            work)
      }
    }
  }

  it("inserts an Sierra identified invisible Work into the index") {
    val work = createIdentifiedInvisibleWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier
    )

    withLocalElasticsearchIndex(itemType = itemType) { esIndex =>
      withIngestorWorkerService(esIndex) {
        case (QueuePair(queue, _), bucket) =>
          sendMessage[IdentifiedBaseWork](
            bucket = bucket,
            queue = queue,
            obj = work)

          assertElasticsearchEventuallyHasWork(
            indexName = esIndex,
            itemType = itemType,
            work)
      }
    }
  }

  it("inserts an Sierra identified redirected Work into the index") {
    val work = createIdentifiedRedirectedWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier
    )

    withLocalElasticsearchIndex(itemType = itemType) { esIndex =>
      withIngestorWorkerService(esIndex) {
        case (QueuePair(queue, _), bucket) =>
          sendMessage[IdentifiedBaseWork](
            bucket = bucket,
            queue = queue,
            obj = work)

          assertElasticsearchEventuallyHasWork(
            indexName = esIndex,
            itemType = itemType,
            work)
      }
    }
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

    withLocalElasticsearchIndex(itemType = itemType) { esIndex =>
      withIngestorWorkerService(esIndex) {
        case (QueuePair(queue, dlq), bucket) =>
          works.foreach { work =>
            sendMessage[IdentifiedBaseWork](
              bucket = bucket,
              queue = queue,
              obj = work)
          }

          assertElasticsearchEventuallyHasWork(
            indexName = esIndex,
            itemType = itemType,
            works: _*)

          assertQueueEmpty(dlq)
      }
    }
  }

  it("Inserts a non sierra or miro identified work") {
    val work = createIdentifiedWorkWith(
      sourceIdentifier = createSourceIdentifierWith(
        identifierType = IdentifierType("calm-altref-no")
      )
    )

    withLocalElasticsearchIndex(itemType = itemType) { esIndex =>
      withIngestorWorkerService(esIndex) {
        case (QueuePair(queue, dlq), bucket) =>
          sendMessage[IdentifiedBaseWork](
            bucket = bucket,
            queue = queue,
            obj = work)

          assertElasticsearchEventuallyHasWork(
            indexName = esIndex,
            itemType = itemType,
            work)

          eventually {
            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)
          }
      }
    }
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

    withLocalElasticsearchIndex(itemType = itemType) { esIndex =>
      withIngestorWorkerService(esIndex) {
        case (QueuePair(queue, dlq), bucket) =>
          works.foreach { work =>
            sendMessage[IdentifiedBaseWork](
              bucket = bucket,
              queue = queue,
              obj = work)
          }

          assertElasticsearchEventuallyHasWork(
            indexName = esIndex,
            itemType = itemType,
            miroWork,
            sierraWork,
            otherWork)

          eventually {
            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)
          }
      }
    }
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

    withLocalElasticsearchIndex(itemType = itemType) { esIndex =>
      insertIntoElasticsearch(
        indexName = esIndex,
        itemType = itemType,
        newSierraWork)
      withIngestorWorkerService(esIndex) {
        case (QueuePair(queue, dlq), bucket) =>
          works.foreach { work =>
            sendMessage[IdentifiedBaseWork](
              bucket = bucket,
              queue = queue,
              obj = work)
          }

          assertElasticsearchEventuallyHasWork(
            indexName = esIndex,
            itemType = itemType,
            sierraWork,
            newSierraWork)
          eventually {
            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)
          }
      }
    }
  }

  it("ingests lots of works") {
    val works = createIdentifiedWorks(count = 250)

    withLocalElasticsearchIndex(itemType = itemType) { esIndex =>
      withIngestorWorkerService(esIndex) {
        case (QueuePair(queue, dlq), bucket) =>
          works.foreach { work =>
            sendMessage[IdentifiedBaseWork](
              bucket = bucket,
              queue = queue,
              obj = work)
          }

          eventually {
            works.foreach { work =>
              assertElasticsearchEventuallyHasWork(
                indexName = esIndex,
                itemType = itemType,
                work)
            }
          }

          eventually {
            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)
          }
      }
    }
  }

  it("only deletes successfully ingested works from the queue") {
    val subsetOfFieldsIndex =
      new SubsetOfFieldsWorksIndex(elasticClient, itemType)

    val work = createIdentifiedWork
    val workDoesNotMatchMapping = createIdentifiedWorkWith(
      subjects = List(Subject(label = "crystallography", concepts = Nil))
    )

    val works = List(work, workDoesNotMatchMapping)

    withLocalElasticsearchIndex(
      subsetOfFieldsIndex,
      indexName = (Random.alphanumeric take 10 mkString) toLowerCase) {
      esIndex =>
        withIngestorWorkerService(esIndex) {
          case (QueuePair(queue, dlq), bucket) =>
            works.foreach { work =>
              sendMessage[IdentifiedBaseWork](
                bucket = bucket,
                queue = queue,
                obj = work)
            }

            assertElasticsearchNeverHasWork(
              indexName = esIndex,
              itemType = itemType,
              workDoesNotMatchMapping)
            assertElasticsearchEventuallyHasWork(
              indexName = esIndex,
              itemType = itemType,
              work)
            eventually {
              assertQueueEmpty(queue)
              assertQueueHasSize(dlq, 1)
            }
        }
    }
  }

  it("returns a failed Future if indexing into Elasticsearch fails") {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withLocalSqsQueueAndDlq {
          case queuePair @ QueuePair(queue, dlq) =>
            withLocalS3Bucket { bucket =>
              withMessageStream[IdentifiedBaseWork, Assertion](
                actorSystem,
                bucket,
                queue,
                metricsSender) { messageStream =>
                val brokenRestClient: RestClient = RestClient
                  .builder(new HttpHost("localhost", 9800, "http"))
                  .setHttpClientConfigCallback(
                    new ElasticCredentials("elastic", "changeme"))
                  .build()

                val brokenElasticClient: HttpClient =
                  HttpClient.fromRestClient(brokenRestClient)

                val brokenWorkIndexer = new WorkIndexer(
                  elasticClient = brokenElasticClient
                )

                withIngestorWorkerService[Assertion](
                  esIndex = "works-v1",
                  actorSystem,
                  brokenWorkIndexer,
                  messageStream) { _ =>
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
      }
    }
  }

  private def withIngestorWorkerService[R](esIndex: String)(
    testWith: TestWith[(QueuePair, Bucket), R]): R = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withLocalSqsQueueAndDlqAndTimeout(10) {
          case queuePair @ QueuePair(queue, dlq) =>
            withLocalS3Bucket { bucket =>
              withWorkIndexer[R](elasticClient = elasticClient) { workIndexer =>
                withMessageStream[IdentifiedBaseWork, R](
                  actorSystem,
                  bucket,
                  queue,
                  metricsSender) { messageStream =>
                  withIngestorWorkerService[R](
                    esIndex,
                    actorSystem,
                    workIndexer,
                    messageStream) { _ =>
                    testWith((queuePair, bucket))
                  }
                }
              }
            }
        }
      }
    }
  }

  private def withIngestorWorkerService[R](
    esIndex: String,
    actorSystem: ActorSystem,
    workIndexer: WorkIndexer,
    messageStream: MessageStream[IdentifiedBaseWork])(
    testWith: TestWith[IngestorWorkerService, R]): R = {

    val ingestorConfig = IngestorConfig(
      batchSize = 100,
      flushInterval = 5 seconds,
      elasticConfig = IngestElasticConfig(
        documentType = itemType,
        indexName = esIndex
      )
    )

    val ingestorWorkerService = new IngestorWorkerService(
      ingestorConfig = ingestorConfig,
      identifiedWorkIndexer = workIndexer,
      messageStream = messageStream,
      system = actorSystem
    )

    testWith(ingestorWorkerService)
  }
}
