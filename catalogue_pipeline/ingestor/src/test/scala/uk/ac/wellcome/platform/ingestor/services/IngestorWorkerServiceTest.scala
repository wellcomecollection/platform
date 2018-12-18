package uk.ac.wellcome.platform.ingestor.services

import com.sksamuel.elastic4s.http.ElasticClient
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.ElasticCredentials
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.fixtures.{Messaging, SQS}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.{IdentifiedBaseWork, IdentifierType}
import uk.ac.wellcome.platform.ingestor.fixtures.WorkerServiceFixture

class IngestorWorkerServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with Messaging
    with ElasticsearchFixtures
    with SQS
    with WorkerServiceFixture
    with WorksGenerators {

  it("indexes a Miro identified Work") {
    val miroSourceIdentifier = createSourceIdentifier

    val work = createIdentifiedWorkWith(sourceIdentifier = miroSourceIdentifier)

    assertWorksIndexedCorrectly(work)
  }

  it("indexes a Sierra identified Work") {
    val work = createIdentifiedWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier
    )

    assertWorksIndexedCorrectly(work)
  }

  it("indexes a Sierra identified invisible Work") {
    val work = createIdentifiedInvisibleWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier
    )

    assertWorksIndexedCorrectly(work)
  }

  it("indexes a Sierra identified redirected Work") {
    val work = createIdentifiedRedirectedWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier
    )

    assertWorksIndexedCorrectly(work)
  }

  it("indexes a mixture of Miro and Sierra works") {
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

    assertWorksIndexedCorrectly(works: _*)
  }

  it("inserts a non Sierra- or Miro- identified work") {
    val work = createIdentifiedWorkWith(
      sourceIdentifier = createSourceIdentifierWith(
        identifierType = IdentifierType("calm-altref-no")
      )
    )

    assertWorksIndexedCorrectly(work)
  }

  it("indexes a mixture of Miro and Sierra, and otherly-identified Works") {
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

    assertWorksIndexedCorrectly(works: _*)
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

    assertWorksIndexedCorrectly(works: _*)
  }

  it("ingests lots of works") {
    val works = createIdentifiedWorks(count = 250)

    assertWorksIndexedCorrectly(works: _*)
  }

  ignore("only deletes successfully ingested works from the queue") {
    case class Shape(sides: Int, colour: String)
    val square = Shape(sides = 4, colour = "red")

    val work = createIdentifiedWork

    withLocalWorksIndex { index =>
      withLocalSqsQueueAndDlq {
        case QueuePair(queue, dlq) =>
          withWorkerService(queue, index) { _ =>
            sendMessage[IdentifiedBaseWork](queue = queue, obj = work)
            sendMessage(queue = queue, obj = square)

            assertElasticsearchEventuallyHasWork(index = index, work)

            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, 1)
          }
      }
    }
  }

  it("returns a failed Future if indexing into Elasticsearch fails") {
    withLocalSqsQueueAndDlq {
      case QueuePair(queue, dlq) =>
        val brokenRestClient: RestClient = RestClient
          .builder(new HttpHost("localhost", 9800, "http"))
          .setHttpClientConfigCallback(
            new ElasticCredentials("elastic", "changeme"))
          .build()

        val brokenElasticClient: ElasticClient =
          ElasticClient.fromRestClient(brokenRestClient)

        withWorkerService(
          queue,
          index = "works-v1",
          elasticClient = brokenElasticClient) { _ =>
          val work = createIdentifiedWork

          sendMessage[IdentifiedBaseWork](queue = queue, obj = work)

          eventually {
            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, 1)
          }
        }
    }
  }

  private def assertWorksIndexedCorrectly(
    works: IdentifiedBaseWork*): Assertion =
    withLocalWorksIndex { index =>
      withLocalSqsQueueAndDlqAndTimeout(visibilityTimeout = 10) {
        case QueuePair(queue, dlq) =>
          withWorkerService(queue, index) { _ =>
            works.map { work =>
              sendMessage[IdentifiedBaseWork](queue = queue, obj = work)
            }

            assertElasticsearchEventuallyHasWork(index = index, works: _*)

            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)
          }
      }
    }
}
