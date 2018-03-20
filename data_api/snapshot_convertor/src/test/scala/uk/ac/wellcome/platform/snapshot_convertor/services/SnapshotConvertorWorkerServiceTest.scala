package uk.ac.wellcome.platform.snapshot_convertor.services

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sqs.AmazonSQS
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SNSConfig, SQSConfig, SQSMessage}
import uk.ac.wellcome.platform.snapshot_convertor.fixtures.ConvertorServiceFixture
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.test.fixtures._

import scala.concurrent.duration._

class SnapshotConvertorWorkerServiceTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with SnsFixtures
    with SqsFixtures
    with AkkaFixtures
    with ScalaFutures
    with ConvertorServiceFixture {

  def withSnapshotConvertorWorkerService[R](
    topicArn: String,
    queueUrl: String
  )(testWith: TestWith[SnapshotConvertorWorkerService, R]) = {

    val metricsSender: MetricsSender = new MetricsSender(
      namespace = "record-receiver-tests",
      100 milliseconds,
      mock[AmazonCloudWatch],
      ActorSystem()
    )

    val sqsReader = new SQSReader(
      sqsClient = sqsClient,
      sqsConfig = SQSConfig(
        queueUrl = queueUrl,
        waitTime = 1 second,
        maxMessages = 1
      )
    )

    val snsWriter = new SNSWriter(
      snsClient = snsClient,
      snsConfig = SNSConfig(topicArn = topicArn)
    )

    withConvertorService { fixtures =>

      val snapshotConvertorWorkerService = new SnapshotConvertorWorkerService(
        fixtures.convertorService,
        sqsReader,
        snsWriter,
        fixtures.actorSystem,
        metricsSender
      )

      testWith(snapshotConvertorWorkerService)
    }
  }

  it(
    "returns a successful Future if the snapshot conversion completes correctly") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topicArn =>
        withLocalS3Bucket { bucketName =>
          val key = "elastic_dump_example.txt.gz"
          val input =
            getClass.getResourceAsStream("/elastic_dump_example.txt.gz")
          val metadata = new ObjectMetadata()

          s3Client.putObject(bucketName, key, input, metadata)

          withSnapshotConvertorWorkerService(topicArn, queueUrl) {
            service =>
              val sqsMessage = SQSMessage(
                subject = None,
                body = "<xml>What is JSON.</xl?>",
                topic = "topic",
                messageType = "message",
                timestamp = "now"
              )

              val future = service.processMessage(message = sqsMessage)

              whenReady(future) { _ =>
                false shouldBe true
              }
          }
        }
      }
    }
  }
}
