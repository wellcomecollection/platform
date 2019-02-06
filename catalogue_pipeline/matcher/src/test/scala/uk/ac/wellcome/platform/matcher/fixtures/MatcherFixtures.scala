package uk.ac.wellcome.platform.matcher.fixtures

import java.time.Instant

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.apache.commons.codec.digest.DigestUtils
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.matcher.locking.{
  DynamoLockingService,
  DynamoRowLockDao,
  RowLock
}
import uk.ac.wellcome.platform.matcher.matcher.WorkMatcher
import uk.ac.wellcome.platform.matcher.services.MatcherWorkerService
import uk.ac.wellcome.platform.matcher.storage.{WorkGraphStore, WorkNodeDao}
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.{LocalDynamoDb, S3}
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

import scala.concurrent.ExecutionContext.Implicits.global

trait MatcherFixtures
    extends Akka
    with Messaging
    with SNS
    with LocalWorkGraphDynamoDb
    with MetricsSenderFixture
    with S3 {

  implicit val instantLongFormat: AnyRef with DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, Long, IllegalArgumentException](
      Instant.ofEpochSecond
    )(
      _.getEpochSecond
    )

  def withWorkerService[R](queue: SQS.Queue, topic: Topic, graphTable: Table)(
    testWith: TestWith[MatcherWorkerService, R])(
    implicit objectStore: ObjectStore[TransformedBaseWork]): R =
    withSNSWriter(topic) { snsWriter =>
      withActorSystem { implicit actorSystem =>
        withMockMetricSender { metricsSender =>
          withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
            withWorkGraphStore(graphTable) { workGraphStore =>
              withWorkMatcher(workGraphStore, lockTable, metricsSender) {
                workMatcher =>
                  withMessageStream[TransformedBaseWork, R](
                    queue = queue,
                    metricsSender = metricsSender
                  ) { messageStream =>
                    val workerService = new MatcherWorkerService(
                      messageStream = messageStream,
                      snsWriter = snsWriter,
                      workMatcher = workMatcher
                    )

                    workerService.run()

                    testWith(workerService)
                  }
              }
            }
          }
        }
      }
    }

  def withWorkerService[R](queue: SQS.Queue, topic: Topic)(
    testWith: TestWith[MatcherWorkerService, R])(
    implicit objectStore: ObjectStore[TransformedBaseWork]): R =
    withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { graphTable =>
      withWorkerService(queue, topic, graphTable) { service =>
        testWith(service)
      }
    }

  def withWorkMatcher[R](
    workGraphStore: WorkGraphStore,
    lockTable: Table,
    metricsSender: MetricsSender)(testWith: TestWith[WorkMatcher, R]): R =
    withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>
      withLockingService(dynamoRowLockDao, metricsSender) { lockingService =>
        val workMatcher = new WorkMatcher(
          workGraphStore = workGraphStore,
          lockingService = lockingService
        )
        testWith(workMatcher)
      }
    }

  def withDynamoRowLockDao[R](dynamoDbClient: AmazonDynamoDB, lockTable: Table)(
    testWith: TestWith[DynamoRowLockDao, R]): R = {
    val dynamoRowLockDao = new DynamoRowLockDao(
      dynamoDBClient = dynamoDbClient,
      dynamoConfig = createDynamoConfigWith(lockTable)
    )
    testWith(dynamoRowLockDao)
  }

  def withDynamoRowLockDao[R](lockTable: Table)(
    testWith: TestWith[DynamoRowLockDao, R]): R =
    withDynamoRowLockDao(dynamoDbClient, lockTable = lockTable) { rowLockDao =>
      testWith(rowLockDao)
    }

  def withLockingService[R](dynamoRowLockDao: DynamoRowLockDao,
                            metricsSender: MetricsSender)(
    testWith: TestWith[DynamoLockingService, R]): R = {
    val lockingService =
      new DynamoLockingService(dynamoRowLockDao, metricsSender)
    testWith(lockingService)
  }

  def withWorkMatcherAndLockingService[R](workGraphStore: WorkGraphStore,
                                          lockingService: DynamoLockingService)(
    testWith: TestWith[WorkMatcher, R]): R = {
    val workMatcher = new WorkMatcher(workGraphStore, lockingService)
    testWith(workMatcher)
  }

  def withWorkGraphStore[R](graphTable: Table)(
    testWith: TestWith[WorkGraphStore, R]): R = {
    withWorkNodeDao(graphTable) { workNodeDao =>
      val workGraphStore = new WorkGraphStore(workNodeDao)
      testWith(workGraphStore)
    }
  }

  def withWorkNodeDao[R](table: Table)(
    testWith: TestWith[WorkNodeDao, R]): R = {
    val workNodeDao = new WorkNodeDao(
      dynamoDbClient,
      DynamoConfig(table = table.name, index = table.index)
    )
    testWith(workNodeDao)
  }

  def ciHash(str: String): String = {
    DigestUtils.sha256Hex(str)
  }

  def aRowLock(id: String, contextId: String) = {
    RowLock(id, contextId, Instant.now, Instant.now.plusSeconds(100))
  }

  def assertNoRowLocks(lockTable: LocalDynamoDb.Table) = {
    Scanamo.scan[RowLock](dynamoDbClient)(lockTable.name) shouldBe empty
  }

}
