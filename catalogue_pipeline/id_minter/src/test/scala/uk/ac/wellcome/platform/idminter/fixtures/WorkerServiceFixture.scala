package uk.ac.wellcome.platform.idminter.fixtures

import io.circe.Json
import scalikejdbc.{ConnectionPool, DB}
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.fixtures.{Messaging, SNS}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.idminter.config.models.IdentifiersTableConfig
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.models.IdentifiersTable
import uk.ac.wellcome.platform.idminter.services.IdMinterWorkerService
import uk.ac.wellcome.platform.idminter.steps.{IdEmbedder, IdentifierGenerator}
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

import scala.concurrent.ExecutionContext.Implicits.global

trait WorkerServiceFixture
    extends Akka
    with IdentifiersDatabase
    with Messaging
    with MetricsSenderFixture
    with SNS {
  def withWorkerService[R](bucket: Bucket,
                           topic: Topic,
                           queue: Queue,
                           identifiersDao: IdentifiersDao,
                           identifiersTableConfig: IdentifiersTableConfig)(
    testWith: TestWith[IdMinterWorkerService, R]): R =
    withActorSystem { implicit actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withMessageWriter[Json, R](bucket, topic, snsClient) { messageWriter =>
          withMessageStream[Json, R](queue, metricsSender) { messageStream =>
            val workerService = new IdMinterWorkerService(
              idEmbedder = new IdEmbedder(
                identifierGenerator = new IdentifierGenerator(
                  identifiersDao = identifiersDao
                )
              ),
              writer = messageWriter,
              messageStream = messageStream,
              rdsClientConfig = rdsClientConfig,
              identifiersTableConfig = identifiersTableConfig
            )

            workerService.run()

            testWith(workerService)
          }
        }
      }
    }

  def withWorkerService[R](bucket: Bucket,
                           topic: Topic,
                           queue: Queue,
                           identifiersTableConfig: IdentifiersTableConfig)(
    testWith: TestWith[IdMinterWorkerService, R]): R = {
    Class.forName("com.mysql.jdbc.Driver")
    ConnectionPool.singleton(s"jdbc:mysql://$host:$port", username, password)

    val identifiersDao = new IdentifiersDao(
      db = DB.connect(),
      identifiers = new IdentifiersTable(
        identifiersTableConfig = identifiersTableConfig
      )
    )
    withWorkerService(
      bucket,
      topic,
      queue,
      identifiersDao,
      identifiersTableConfig) { service =>
      testWith(service)
    }
  }
}
