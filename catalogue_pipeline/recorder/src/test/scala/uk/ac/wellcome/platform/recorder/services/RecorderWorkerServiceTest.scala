package uk.ac.wellcome.platform.recorder.services

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.metrics.MetricsSender
import uk.ac.wellcome.messaging.sns.{SNSConfig, SNSWriter}
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSMessage, SQSReader}
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.platform.recorder.models.RecorderWorkEntry
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.work.internal.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class RecorderWorkerServiceTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with Akka
    with LocalVersionedHybridStore
    with SQS
    with ScalaFutures {

  val title = "Whose umbrella did I find?"

  val sourceIdentifier = SourceIdentifier(
    identifierScheme = IdentifierSchemes.miroImageNumber,
    value = "U8634924",
    ontologyType = "Work"
  )

  val work = UnidentifiedWork(
    title = Some(title),
    sourceIdentifier = sourceIdentifier,
    identifiers = List(sourceIdentifier),
    version = 2
  )

  def withRecorderWorkerService(table: Table, bucket: Bucket)(
    testWith: TestWith[RecorderWorkerService, Assertion]) = {
    withActorSystem { actorSystem =>
      val metricsSender = new MetricsSender(
        namespace = "recorder-worker-service-test",
        flushInterval = 100 milliseconds,
        amazonCloudWatch = mock[AmazonCloudWatch],
        actorSystem = actorSystem
      )

      withLocalSqsQueue { queue =>
        withVersionedHybridStore[RecorderWorkEntry, Unit](bucket = bucket, table = table) { versionedHybridStore =>
          val workerService = new RecorderWorkerService(
            versionedHybridStore = versionedHybridStore,
            reader = new SQSReader(
              sqsClient = sqsClient,
              sqsConfig = SQSConfig(
                queueUrl = queue.url,
                waitTime = 1 second,
                maxMessages = 1
              )
            ),
            system = actorSystem,
            metrics = metricsSender
          )

          try {
            testWith(workerService)
          } finally {
            workerService.stop()
          }
        }
      }
    }
  }

  it("returns a successful Future if the Work is recorded successfully") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withRecorderWorkerService(table, bucket) { service =>
          val future = service.store(work = work)

          whenReady(future) { _ =>
            val actualRecords: List[RecorderWorkEntry] =
              Scanamo
                .scan[RecorderWorkEntry](dynamoDbClient)(table.name)
                .map(_.right.get)

            println(actualRecords)
            1 shouldBe 0
          }
        }
      }
    }
  }
}
