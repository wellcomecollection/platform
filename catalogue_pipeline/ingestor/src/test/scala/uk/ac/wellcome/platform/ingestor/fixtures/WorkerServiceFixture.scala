package uk.ac.wellcome.platform.ingestor.fixtures

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.http.ElasticClient
import org.scalatest.Suite
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.Messaging
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork
import uk.ac.wellcome.platform.ingestor.config.models.IngestorConfig
import uk.ac.wellcome.platform.ingestor.services.IngestorWorkerService
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait WorkerServiceFixture
    extends ElasticsearchFixtures
    with Messaging
    with Akka {
  this: Suite =>
  def withWorkerService[R](queue: Queue,
                           index: Index,
                           elasticClient: ElasticClient = elasticClient)(
    testWith: TestWith[IngestorWorkerService, R]): R =
    withActorSystem { implicit actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withMessageStream[IdentifiedBaseWork, R](
          queue = queue,
          metricsSender = metricsSender) { messageStream =>
          val ingestorConfig = IngestorConfig(
            batchSize = 100,
            flushInterval = 5 seconds,
            index = index
          )

          val workerService = new IngestorWorkerService(
            elasticClient = elasticClient,
            ingestorConfig = ingestorConfig,
            messageStream = messageStream
          )

          workerService.run()

          testWith(workerService)
        }
      }
    }
}
