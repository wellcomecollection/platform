package uk.ac.wellcome.message

import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.utils.JsonUtil._
import org.scalatest.{FunSpec, Matchers}
import org.mockito.Matchers.{any, matches}
import org.mockito.Mockito.{times, verify}
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.models.aws.{S3Config, SNSConfig}
import uk.ac.wellcome.s3.S3ObjectStore
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.test.fixtures.Messaging
import uk.ac.wellcome.test.utils.ExtendedPatience

class MessagingIntegrationTest
    extends FunSpec
    with Matchers
    with Messaging
    with Eventually
    with ExtendedPatience {

  def withFixtures[R] =
    withActorSystem[R] and
      withLocalStackSqsQueue[R] and
      withMetricsSender[R] _ and
      withLocalS3Bucket[R] and
      withMessageWorker[R](
        localStackSqsClient,
        s3Client
      ) _

  it("sends and receives messages") {
    withFixtures {
      case (_, queue, metrics, bucket, worker) =>
        withLocalStackSnsTopic { topic =>
          withLocalStackSubscription(queue, topic) { _ =>
            val s3Config = S3Config(bucketName = bucket.name)

            val snsConfig = SNSConfig(topic.arn)
            val snsWriter = new SNSWriter(localStackSnsClient, snsConfig)

            val s3ObjectStore = new S3ObjectStore[ExampleObject](
              s3Client,
              s3Config,
              keyPrefixGenerator
            )

            val messages =
              new MessageWriter[ExampleObject](
                snsWriter,
                s3Config,
                s3ObjectStore)

            val exampleObject = ExampleObject("some value")

            messages.write(exampleObject, "subject")

            eventually {
              verify(
                metrics,
                times(1)
              ).timeAndCount(
                matches(".*_ProcessMessage"),
                any()
              )
            }
          }
        }
    }
  }
}
