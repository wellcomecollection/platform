package uk.ac.wellcome.platform.ingestor

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.platform.ingestor.fixtures.WorkerServiceFixture

class IngestorIndexTest
    extends FunSpec
    with SQS
    with Matchers
    with ScalaFutures
    with Messaging
    with ElasticsearchFixtures
    with WorkerServiceFixture {

  it("creates the index at startup if it doesn't already exist") {
    val indexName = "works"

    eventuallyDeleteIndex(indexName)

    withLocalSqsQueue { queue =>
      withWorkerService(queue, indexName) { _ =>
        eventuallyIndexExists(indexName)
      }
    }
  }
}
