package uk.ac.wellcome.messaging.message

import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.utils.JsonUtil._
import org.scalatest.{FunSpec, Matchers}
import org.mockito.Matchers.{any, matches}
import org.mockito.Mockito.{times, verify}
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.models.aws.{S3Config, SNSConfig}
import uk.ac.wellcome.s3.S3ObjectStore
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.test.utils.ExtendedPatience

class MessagingIntegrationTest
    extends FunSpec
    with Matchers
    with Messaging
    with Eventually
    with ExtendedPatience {

  it("sends and receives messages") {
    withMessageWorkerFixtures {
      case (_, metrics, queue, bucket, worker) =>
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

            val messageWriter =
              new MessageWriter[ExampleObject](
                snsWriter,
                s3Config,
                s3ObjectStore)

            val exampleObject = ExampleObject("some value")

            messageWriter.write(exampleObject, "subject")

            eventually {
              worker.calledWith shouldBe Some(exampleObject)
            }
          }
        }
    }
  }
}
