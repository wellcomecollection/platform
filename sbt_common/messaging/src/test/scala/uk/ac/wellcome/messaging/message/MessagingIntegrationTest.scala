package uk.ac.wellcome.messaging.message

import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.utils.JsonUtil._
import org.scalatest.{FunSpec, Matchers}
import org.mockito.Matchers.{any, matches}
import org.mockito.Mockito.{times, verify}
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.messaging.sns.{SNSConfig, SNSWriter}
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.models.aws.S3Config
import uk.ac.wellcome.s3.S3ObjectStore
import uk.ac.wellcome.test.utils.ExtendedPatience

class MessagingIntegrationTest
    extends FunSpec
    with Matchers
    with Messaging
    with Eventually
    with ExtendedPatience {

  val message = ExampleObject("A message sent in the MessagingIntegrationTest")
  val subject = "message-integration-test-subject"

  it("sends and receives messages") {
    withMessageWorkerFixtures {
      case (metrics, queue, bucket, worker) =>
        withLocalStackSnsTopic { topic =>
          withLocalStackSubscription(queue, topic) { _ =>
            val s3Config = S3Config(bucketName = bucket.name)
            val snsConfig = SNSConfig(topicArn = topic.arn)
            val messageConfig = MessageConfig(
              s3Config = s3Config,
              snsConfig = snsConfig
            )

            val messageWriter = new MessageWriter[ExampleObject](
              messageConfig = messageConfig,
              snsClient = snsClient,
              s3Client = s3Client,
              keyPrefixGenerator = keyPrefixGenerator
            )

            messageWriter.write(message = message, subject = subject)

            eventually {
              worker.calledWith shouldBe Some(message)
            }
          }
        }
    }
  }
}
