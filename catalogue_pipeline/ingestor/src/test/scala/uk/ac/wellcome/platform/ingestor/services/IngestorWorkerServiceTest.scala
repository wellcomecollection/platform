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
import uk.ac.wellcome.models.work.internal.{
  IdentifiedWork,
  IdentifierType,
  SourceIdentifier
}
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.platform.ingestor.fixtures.WorkIndexerFixtures
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class IngestorWorkerServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with ElasticsearchFixtures
    with SQS
    with S3
    with WorkIndexerFixtures
    with Messaging
    with WorksUtil {

  val itemType = "work"

  it("inserts an Miro identified Work into v1 and v2 indices") {
    val miroSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      ontologyType = "Work",
      value = "M000765"
    )

    val work = createWork().copy(sourceIdentifier = miroSourceIdentifier)

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) {
          case (QueuePair(queue, _), bucket) =>
            val messageBody = put[IdentifiedWork](
              obj = work,
              location = ObjectLocation(
                namespace = bucket.name,
                key = s"work.json"
              )
            )
            sqsClient.sendMessage(queue.url, messageBody)

            assertElasticsearchEventuallyHasWork(indexName = esIndexV1, itemType = itemType, work)

            assertElasticsearchEventuallyHasWork(indexName = esIndexV2, itemType = itemType, work)
        }
      }
    }
  }

  it("inserts an Sierra identified Work only into the v2 index") {
    val sierraSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("sierra-system-number"),
      ontologyType = "Work",
      value = "b1027467"
    )

    val work = createWork().copy(sourceIdentifier = sierraSourceIdentifier)

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) {
          case (QueuePair(queue, _), bucket) =>
            val messageBody = put[IdentifiedWork](
              obj = work,
              location = ObjectLocation(
                namespace = bucket.name,
                key = s"work.json"
              )
            )
            sqsClient.sendMessage(queue.url, messageBody)

            assertElasticsearchNeverHasWork(indexName = esIndexV1, itemType = itemType, work)

            assertElasticsearchEventuallyHasWork(indexName = esIndexV2, itemType = itemType, work)
        }
      }
    }
  }

  it("inserts a mixture of miro and sierra works into the correct indices") {
    val miroWork1 = createWork().copy(sourceIdentifier = createIdentifier("miro-image-number", "M1"), canonicalId = "m1")
    val miroWork2 = createWork().copy(sourceIdentifier = createIdentifier("miro-image-number", "M2"), canonicalId = "m2")
    val sierraWork1 = createWork().copy(sourceIdentifier = createIdentifier("sierra-system-number", "S1"), canonicalId = "s1")
    val sierraWork2 = createWork().copy(sourceIdentifier = createIdentifier("sierra-system-number", "S2"), canonicalId = "s2")


    val works = List(miroWork1, miroWork2, sierraWork1, sierraWork2)


    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) {
          case (QueuePair(queue, _), bucket) =>

            works.foreach { work =>
              val messageBody = put[IdentifiedWork](
                obj = work,
                location = ObjectLocation(
                  namespace = bucket.name,
                  key = s"${work.canonicalId}.json"
                )
              )

              sqsClient.sendMessage(queue.url, messageBody)
            }

            assertElasticsearchNeverHasWork(indexName = esIndexV1, itemType = itemType, sierraWork1, sierraWork2)

            assertElasticsearchEventuallyHasWork(indexName = esIndexV2, itemType = itemType, works: _*)
            assertElasticsearchEventuallyHasWork(indexName = esIndexV1, itemType = itemType, miroWork1, miroWork2)
        }
      }
    }

  }

  private def createIdentifier(identifierType: String, value: String) = {
    SourceIdentifier(
      identifierType = IdentifierType(identifierType),
      ontologyType = "Work",
      value = value
    )
  }

  it("fails inserting a non sierra or miro identified work") {
    val calmSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("calm-altref-no"),
      ontologyType = "Work",
      value = "MS/237"
    )

    val work = createWork().copy(sourceIdentifier = calmSourceIdentifier)

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) {
          case (QueuePair(queue, dlq), bucket) =>
            val messageBody = put[IdentifiedWork](
              obj = work,
              location = ObjectLocation(
                namespace = bucket.name,
                key = s"work.json"
              )
            )
            sqsClient.sendMessage(queue.url, messageBody)

            eventually {
              assertQueueEmpty(queue)
              assertQueueHasSize(dlq, 1)
            }
        }
      }
    }
  }

  it("inserts a mixture of miro and sierra works into the correct indices and sends invalid messages to the dlq") {
    val miroWork = createWork().copy(sourceIdentifier = createIdentifier("miro-image-number", "M1"))
    val sierraWork = createWork().copy(sourceIdentifier = createIdentifier("sierra-system-number", "S2"))
    val invalidWork = createWork().copy(sourceIdentifier = createIdentifier("calm-system-number", "C1"))

    val works = List(miroWork, sierraWork, invalidWork)

    withLocalElasticsearchIndex(itemType = itemType) { esIndexV1 =>
      withLocalElasticsearchIndex(itemType = itemType) { esIndexV2 =>
        withIngestorWorkerService(esIndexV1, esIndexV2) {
          case (QueuePair(queue, dlq), bucket) =>

            works.foreach { work =>
              val messageBody = put[IdentifiedWork](
                obj = work,
                location = ObjectLocation(
                  namespace = bucket.name,
                  key = s"${work.canonicalId}.json"
                )
              )

              sqsClient.sendMessage(queue.url, messageBody)
            }

            assertElasticsearchNeverHasWork(indexName = esIndexV1, itemType = itemType, sierraWork)

            assertElasticsearchEventuallyHasWork(indexName = esIndexV2, itemType = itemType, miroWork, sierraWork)
            assertElasticsearchEventuallyHasWork(indexName = esIndexV1, itemType = itemType, miroWork)
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
              withMessageStream[IdentifiedWork, Assertion](
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
                  val miroSourceIdentifier = SourceIdentifier(
                    identifierType = IdentifierType("miro-image-number"),
                    ontologyType = "Work",
                    value = "B000675"
                  )

                  val work =
                    createWork().copy(sourceIdentifier = miroSourceIdentifier)

                  val messageBody = put[IdentifiedWork](
                    obj = work,
                    location = ObjectLocation(
                      namespace = bucket.name,
                      key = s"work.json"
                    )
                  )
                  sqsClient.sendMessage(queue.url, messageBody)

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
        withLocalSqsQueueAndDlq {
          case queuePair @ QueuePair(queue, dlq) =>
            withLocalS3Bucket { bucket =>
              withWorkIndexer[R](elasticClient = elasticClient) {
                workIndexer =>
                  withMessageStream[IdentifiedWork, R](
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
    messageStream: MessageStream[IdentifiedWork])(
    testWith: TestWith[IngestorWorkerService, R]): R = {
    val elasticConfig = ElasticConfig(
      documentType = itemType,
      indexV1name = esIndexV1,
      indexV2name = esIndexV2
    )

    val ingestorWorkerService = new IngestorWorkerService(
      elasticConfig = elasticConfig,
      identifiedWorkIndexer = workIndexer,
      messageStream = messageStream,
      system = actorSystem
    )

    testWith(ingestorWorkerService)
  }
}
