package uk.ac.wellcome.platform.ingestor.fixtures

import com.sksamuel.elastic4s.http.HttpClient
import org.scalatest.Suite
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork
import uk.ac.wellcome.platform.ingestor.config.models.{
  IngestElasticConfig,
  IngestorConfig
}
import uk.ac.wellcome.platform.ingestor.services.IngestorWorkerService
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait WorkerServiceFixture extends ElasticsearchFixtures with Messaging {
  this: Suite =>
  def withWorkerService[R](queue: Queue,
                           indexName: String,
                           elasticClient: HttpClient = elasticClient)(
    testWith: TestWith[IngestorWorkerService, R]): R =
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withMessageStream[IdentifiedBaseWork, R](
          actorSystem,
          queue,
          metricsSender) { messageStream =>
          val ingestorConfig = IngestorConfig(
            batchSize = 100,
            flushInterval = 5 seconds,
            elasticConfig = IngestElasticConfig(
              documentType = documentType,
              indexName = indexName
            )
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
