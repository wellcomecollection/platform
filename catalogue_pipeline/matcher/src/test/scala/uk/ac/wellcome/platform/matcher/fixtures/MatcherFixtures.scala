package uk.ac.wellcome.platform.matcher.fixtures

import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig, SNSWriter}
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSStream}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.matcher.Server
import uk.ac.wellcome.platform.matcher.matcher.LinkedWorkMatcher
import uk.ac.wellcome.platform.matcher.messages.MatcherMessageReceiver
import uk.ac.wellcome.platform.matcher.storage.{LinkedWorkDao, WorkGraphStore}
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3.{S3Config, S3TypeStore}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

trait MatcherFixtures extends Akka with SQS with SNS with LocalLinkedWorkDynamoDb with MetricsSenderFixture with S3 {

  def withMatcherServer[R](
                            queue: Queue,
                            bucket: Bucket,
                            topic: Topic,
                            table: Table
                          )(testWith: TestWith[EmbeddedHttpServer, R]) = {

    val server: EmbeddedHttpServer =
      new EmbeddedHttpServer(
        new Server(),
        flags =
          cloudWatchLocalFlags ++
            s3LocalFlags(bucket) ++
            sqsLocalFlags(queue) ++
            snsLocalFlags(topic) ++
            dynamoDbLocalEndpointFlags(table)
      )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }

  def withMatcherMessageReceiver[R](
                                     queue: SQS.Queue,
                                     storageBucket: Bucket,
                                     topic: Topic)(testWith: TestWith[MatcherMessageReceiver, R]) = {
    val storageS3Config = S3Config(storageBucket.name)
    val snsWriter =
      new SNSWriter(snsClient, SNSConfig(topic.arn))


    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withLocalDynamoDbTable { table =>
          withWorkGraphStore(table) { workGraphStore =>
            withLinkedWorkMatcher(table, workGraphStore) { linkedWorkMatcher =>
              implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
              val sqsStream = new SQSStream[NotificationMessage](
                actorSystem = actorSystem,
                sqsClient = asyncSqsClient,
                sqsConfig = SQSConfig(queue.url, 1 second, 1),
                metricsSender = metricsSender
              )
              val matcherMessageReceiver = new MatcherMessageReceiver(
                sqsStream,
                snsWriter,
                new S3TypeStore[RecorderWorkEntry](s3Client),
                storageS3Config,
                actorSystem,
                linkedWorkMatcher)
              testWith(matcherMessageReceiver)
            }
          }
        }
      }
    }
  }

  def withLinkedWorkMatcher[R](table: Table, workGraphStore: WorkGraphStore)(
    testWith: TestWith[LinkedWorkMatcher, R]): R = {
    val linkedWorkMatcher = new LinkedWorkMatcher(workGraphStore)
    testWith(linkedWorkMatcher)
  }

  def withWorkGraphStore[R](table: Table)(
    testWith: TestWith[WorkGraphStore, R]): R = {
    withLinkedWorkDao(table) { linkedWorkDao =>
      val workGraphStore = new WorkGraphStore(
        linkedWorkDao)
      testWith(workGraphStore)
    }
  }

  def withLinkedWorkDao[R](table: Table)(
    testWith: TestWith[LinkedWorkDao, R]): R = {
    val linkedDao = new LinkedWorkDao(
      dynamoDbClient,
      DynamoConfig(table.name, Some(table.index)))
    testWith(linkedDao)
  }

  def aSierraSourceIdentifier(id: String) =
    SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "Work", id)

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
