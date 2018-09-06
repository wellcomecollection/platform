package uk.ac.wellcome.platform.ingestor.services

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.http.HttpClient
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.elasticsearch.{ElasticConfig, ElasticCredentials}
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.models.work.internal.{IdentifiedBaseWork, Subject}
import uk.ac.wellcome.models.work.test.util.WorksGenerators
import uk.ac.wellcome.platform.ingestor.IngestorConfig
import uk.ac.wellcome.platform.ingestor.fixtures.WorkIndexerFixtures
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
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

  it("inserts an Miro identified Work into v1 and v2 indices") {
    val miroSourceIdentifier = createSourceIdentifier

    val work = createIdentifiedWorkWith(sourceIdentifier = miroSourceIdentifier)

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) {
          case (QueuePair(queue, _), bucket) =>
            sendMessage[IdentifiedBaseWork](
              bucket = bucket,
              queue = queue,
              obj = work)

            assertElasticsearchEventuallyHasWork(
              indexName = esIndexV1,
              itemType = itemType,
              work)

            assertElasticsearchEventuallyHasWork(
              indexName = esIndexV2,
              itemType = itemType,
              work)
        }
      }
    }
  }

  it("inserts an Sierra identified Work only into the v2 index") {
    val work = createIdentifiedWorkWith(
      sourceIdentifier = createSourceIdentifierWith(
        identifierType = "sierra-system-number"
      )
    )

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) {
          case (QueuePair(queue, _), bucket) =>
            sendMessage[IdentifiedBaseWork](
              bucket = bucket,
              queue = queue,
              obj = work)

            assertElasticsearchNeverHasWork(
              indexName = esIndexV1,
              itemType = itemType,
              work)

            assertElasticsearchEventuallyHasWork(
              indexName = esIndexV2,
              itemType = itemType,
              work)
        }
      }
    }
  }

  it("inserts an Sierra identified invisible Work into the v2 index") {
    val work = createIdentifiedInvisibleWorkWith(
      sourceIdentifier = createSourceIdentifierWith(
        identifierType = "sierra-system-number"
      )
    )

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) {
          case (QueuePair(queue, _), bucket) =>
            sendMessage[IdentifiedBaseWork](
              bucket = bucket,
              queue = queue,
              obj = work)

            assertElasticsearchNeverHasWork(
              indexName = esIndexV1,
              itemType = itemType,
              work)

            assertElasticsearchEventuallyHasWork(
              indexName = esIndexV2,
              itemType = itemType,
              work)
        }
      }
    }
  }

  it("inserts an Sierra identified redirected Work into the v2 index") {
    val work = createIdentifiedRedirectedWorkWith(
      sourceIdentifier = createSourceIdentifierWith(
        identifierType = "sierra-system-number"
      )
    )

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) {
          case (QueuePair(queue, _), bucket) =>
            sendMessage[IdentifiedBaseWork](
              bucket = bucket,
              queue = queue,
              obj = work)

            assertElasticsearchNeverHasWork(
              indexName = esIndexV1,
              itemType = itemType,
              work)

            assertElasticsearchEventuallyHasWork(
              indexName = esIndexV2,
              itemType = itemType,
              work)
        }
      }
    }
  }

  it("inserts a mixture of miro and sierra works into the correct indices") {
    val miroWork1 = createIdentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "miro-image-number")
    )
    val miroWork2 = createIdentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "miro-image-number")
    )
    val sierraWork1 = createIdentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "sierra-system-number")
    )
    val sierraWork2 = createIdentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "sierra-system-number")
    )

    val works = List(miroWork1, miroWork2, sierraWork1, sierraWork2)

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) {
          case (QueuePair(queue, dlq), bucket) =>
            works.foreach { work =>
              sendMessage[IdentifiedBaseWork](
                bucket = bucket,
                queue = queue,
                obj = work)
            }

            assertElasticsearchNeverHasWork(
              indexName = esIndexV1,
              itemType = itemType,
              sierraWork1,
              sierraWork2)

            assertElasticsearchEventuallyHasWork(
              indexName = esIndexV2,
              itemType = itemType,
              works: _*)
            assertElasticsearchEventuallyHasWork(
              indexName = esIndexV1,
              itemType = itemType,
              miroWork1,
              miroWork2)

            assertQueueEmpty(dlq)
        }
      }
    }

  }

  it("fails inserting a non sierra or miro identified work") {
    val work = createIdentifiedWorkWith(
      sourceIdentifier = createSourceIdentifierWith(
        identifierType = "calm-altref-no"
      )
    )

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) {
          case (QueuePair(queue, dlq), bucket) =>
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

  it(
    "inserts a mixture of miro and sierra works into the correct indices and sends invalid messages to the dlq") {
    val miroWork = createIdentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "miro-image-number")
    )
    val sierraWork = createIdentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "sierra-system-number")
    )
    val invalidWork = createIdentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "calm-altref-no")
    )

    val works = List(miroWork, sierraWork, invalidWork)

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) {
          case (QueuePair(queue, dlq), bucket) =>
            works.foreach { work =>
              sendMessage[IdentifiedBaseWork](
                bucket = bucket,
                queue = queue,
                obj = work)
            }

            assertElasticsearchNeverHasWork(
              indexName = esIndexV1,
              itemType = itemType,
              sierraWork)

            assertElasticsearchEventuallyHasWork(
              indexName = esIndexV2,
              itemType = itemType,
              miroWork,
              sierraWork)
            assertElasticsearchEventuallyHasWork(
              indexName = esIndexV1,
              itemType = itemType,
              miroWork)
            eventually {
              assertQueueEmpty(queue)
              assertQueueHasSize(dlq, 1)
            }
        }
      }
    }

  }

  it(
    "deletes successfully ingested works from the queue, including older versions of already ingested works") {
    val sierraWork = createIdentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "sierra-system-number")
    )
    val newSierraWork = createIdentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "sierra-system-number"),
      version = 2
    )
    val oldSierraWork = newSierraWork.copy(version = 1)

    val works = List(sierraWork, oldSierraWork)

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { esIndexV2 =>
        insertIntoElasticsearch(
          indexName = esIndexV2,
          itemType = itemType,
          newSierraWork)
        withIngestorWorkerService(esIndexV1, esIndexV2) {
          case (QueuePair(queue, dlq), bucket) =>
            works.foreach { work =>
              sendMessage[IdentifiedBaseWork](
                bucket = bucket,
                queue = queue,
                obj = work)
            }

            assertElasticsearchEventuallyHasWork(
              indexName = esIndexV2,
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

  }

  it("ingests lots of works") {
    val works = createIdentifiedWorks(count = 250)

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) {
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
                  indexName = esIndexV2,
                  itemType = itemType,
                  work)
                assertElasticsearchEventuallyHasWork(
                  indexName = esIndexV1,
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
  }

  it("only deletes successfully ingested works from the queue") {
    val subsetOfFieldsIndex =
      new SubsetOfFieldsWorksIndex(elasticClient, itemType)

    val work = createIdentifiedWork
    val workDoesNotMatchMapping = createIdentifiedWorkWith(
      subjects = List(Subject(label = "crystallography", concepts = Nil))
    )

    val works = List(work, workDoesNotMatchMapping)

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(
        subsetOfFieldsIndex,
        indexName = (Random.alphanumeric take 10 mkString) toLowerCase) {
        esIndexV2 =>
          withIngestorWorkerService(esIndexV1, esIndexV2) {
            case (QueuePair(queue, dlq), bucket) =>
              works.foreach { work =>
                sendMessage[IdentifiedBaseWork](
                  bucket = bucket,
                  queue = queue,
                  obj = work)
              }

              assertElasticsearchNeverHasWork(
                indexName = esIndexV2,
                itemType = itemType,
                workDoesNotMatchMapping)
              assertElasticsearchEventuallyHasWork(
                indexName = esIndexV2,
                itemType = itemType,
                work)
              eventually {
                assertQueueEmpty(queue)
                assertQueueHasSize(dlq, 1)
              }
          }
      }
    }

  }

  it(
    "does not delete from the queue messages that succeed ingesting into one index but not the other") {
    val subsetOfFieldsIndex =
      new SubsetOfFieldsWorksIndex(elasticClient, itemType)

    val miroWork = createIdentifiedWork
    val miroWorkDoesNotMatchV2Mapping = createIdentifiedWorkWith(
      subjects = List(Subject(label = "crystallography", concepts = Nil))
    )

    val works = List(miroWork, miroWorkDoesNotMatchV2Mapping)

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(
        subsetOfFieldsIndex,
        indexName = (Random.alphanumeric take 10 mkString) toLowerCase) {
        esIndexV2 =>
          withIngestorWorkerService(esIndexV1, esIndexV2) {
            case (QueuePair(queue, dlq), bucket) =>
              works.foreach { work =>
                sendMessage[IdentifiedBaseWork](
                  bucket = bucket,
                  queue = queue,
                  obj = work)
              }

              assertElasticsearchNeverHasWork(
                indexName = esIndexV2,
                itemType = itemType,
                miroWorkDoesNotMatchV2Mapping)
              assertElasticsearchEventuallyHasWork(
                indexName = esIndexV2,
                itemType = itemType,
                miroWork)
              assertElasticsearchEventuallyHasWork(
                indexName = esIndexV1,
                itemType = itemType,
                miroWork,
                miroWorkDoesNotMatchV2Mapping)
              eventually {
                assertQueueEmpty(queue)
                assertQueueHasSize(dlq, 1)
              }
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
                  esIndexV1 = "works-v1",
                  esIndexV2 = "works-v2",
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

  private def withIngestorWorkerService[R](
    esIndexV1: String,
    esIndexV2: String)(testWith: TestWith[(QueuePair, Bucket), R]): R = {
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
                    esIndexV1,
                    esIndexV2,
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
    esIndexV1: String,
    esIndexV2: String,
    actorSystem: ActorSystem,
    workIndexer: WorkIndexer,
    messageStream: MessageStream[IdentifiedBaseWork])(
    testWith: TestWith[IngestorWorkerService, R]): R = {

    val ingestorConfig = IngestorConfig(
      batchSize = 100,
      flushInterval = 5 seconds,
      elasticConfig = ElasticConfig(
        documentType = itemType,
        indexV1name = esIndexV1,
        indexV2name = esIndexV2
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
