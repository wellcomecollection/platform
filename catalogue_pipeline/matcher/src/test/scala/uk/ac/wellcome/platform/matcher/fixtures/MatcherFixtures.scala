package uk.ac.wellcome.platform.matcher.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig, SNSWriter}
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSStream}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.matcher.Server
import uk.ac.wellcome.platform.matcher.lockable.{DynamoLockingService, DynamoLockingServiceConfig}
import uk.ac.wellcome.platform.matcher.matcher.WorkMatcher
import uk.ac.wellcome.platform.matcher.messages.MatcherMessageReceiver
import uk.ac.wellcome.platform.matcher.storage.{WorkGraphStore, WorkNodeDao}
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

import scala.concurrent.duration._

trait MatcherFixtures
    extends Akka
    with SQS
    with SNS
    with LocalWorkGraphDynamoDb
    with MetricsSenderFixture
    with S3 {

  def withMatcherServer[R](
    queue:     Queue,
    bucket:    Bucket,
    topic:     Topic,
    table:     Table,
    lockTable: Table
  )(testWith: TestWith[EmbeddedHttpServer, R]) = {

    val server: EmbeddedHttpServer =
      new EmbeddedHttpServer(
        new Server(),
        flags =
          cloudWatchLocalFlags ++
            s3LocalFlags(bucket) ++
            sqsLocalFlags(queue) ++
            snsLocalFlags(topic) ++
            dynamoDbLocalEndpointFlags(table) ++
            dynamoLockingServiceLocalFlags(lockTable)
      )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }

  def dynamoLockingServiceLocalFlags(table: Table): Map[String, String] =
    Map("aws.dynamo.locking.service.lockTableName" -> table.name)

  def withMatcherMessageReceiver[R](
    queue: SQS.Queue,
    storageBucket: Bucket,
    topic: Topic)(testWith: TestWith[MatcherMessageReceiver, R])(
    implicit objectStore: ObjectStore[RecorderWorkEntry]) = {
    val storageS3Config = S3Config(storageBucket.name)
    val snsWriter =
      new SNSWriter(snsClient, SNSConfig(topic.arn))

    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
          withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { graphTable =>
              withWorkGraphStore(graphTable, lockTable) { workGraphStore =>
                withWorkMatcher(workGraphStore) { workMatcher =>
                  val sqsStream = new SQSStream[NotificationMessage](
                    actorSystem = actorSystem,
                    sqsClient = asyncSqsClient,
                    sqsConfig = SQSConfig(queue.url, 1 second, 1),
                    metricsSender = metricsSender
                  )
                  val matcherMessageReceiver = new MatcherMessageReceiver(
                    sqsStream,
                    snsWriter,
                    objectStore,
                    storageS3Config,
                    actorSystem,
                    workMatcher)
                  testWith(matcherMessageReceiver)
                }
              }
            }
          }
        }
      }
    }


  def withWorkMatcher[R](workGraphStore: WorkGraphStore)(
    testWith: TestWith[WorkMatcher, R]): R = {
    val workMatcher = new WorkMatcher(workGraphStore)
    testWith(workMatcher)
  }

  def withWorkGraphStore[R](graphTable: Table, lockTable: Table)(
    testWith: TestWith[WorkGraphStore, R]): R = {
    withWorkNodeDao(graphTable) { workNodeDao =>
      implicit val lockingService: DynamoLockingService = new DynamoLockingService(dynamoDbClient, DynamoLockingServiceConfig(lockTable.name))
      val workGraphStore = new WorkGraphStore(workNodeDao, lockingService)
      testWith(workGraphStore)
    }
  }

  def withWorkNodeDao[R](table: Table)(testWith: TestWith[WorkNodeDao, R]): R = {
    val workNodeDao = new WorkNodeDao(
      dynamoDbClient,
      DynamoConfig(table = table.name, index = table.index)
    )
    testWith(workNodeDao)
  }

  def aSierraSourceIdentifier(id: String) =
    SourceIdentifier(
      identifierType = IdentifierType("sierra-system-number"),
      "Work",
      id)

  def anUnidentifiedSierraWork: UnidentifiedWork = {
    val sourceIdentifier = aSierraSourceIdentifier("id")
    UnidentifiedWork(
      sourceIdentifier = sourceIdentifier,
      title = Some("WorkTitle"),
      version = 1,
      identifiers = List(sourceIdentifier)
    )
  }
}
