package uk.ac.wellcome.message

import io.circe.Decoder
import uk.ac.wellcome.test.fixtures._
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.test.fixtures.{Akka, Messaging, Metrics, S3, SNS, SQS}

class MessagingIntegrationTest
  extends FunSpec
  with Matchers
  with Metrics
  with Messaging
  with SNS
  with Akka
  with SQS
  with S3 {

  def withMessageReaderFixtures[R] =
    withLocalS3Bucket[R] and
      withMessageReader[R](s3Client) _

  def withMessageWorkerFixtures[R] =
    withActorSystem[R] and
      withLocalSqsQueue[R] and
      withMetricsSender[R] _ and
      withLocalS3Bucket[R] and
      withMessageWorker[R](
        sqsClient, s3Client
      ) _

  it("sends and recieves messages") {
    withMessageWorkerFixtures { case (_, queue, metrics, bucket, worker) =>
      withMessageReaderFixtures { case (bucket, reader) =>

          true shouldBe false

      }
    }
  }
}
