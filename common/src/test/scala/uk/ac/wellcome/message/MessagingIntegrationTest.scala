package uk.ac.wellcome.message

import io.circe.Decoder
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.utils.JsonUtil._
import org.scalatest.{FunSpec, Matchers}
import com.amazonaws.services.sns.util.Topics
import org.mockito.Matchers.{any, matches}
import org.mockito.Mockito.{times, verify}
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.models.aws.{S3Config, SNSConfig}
import uk.ac.wellcome.s3.S3ObjectStore
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.test.fixtures.{Akka, Messaging, Metrics, S3, SNS, SQS}
import uk.ac.wellcome.test.utils.ExtendedPatience

class MessagingIntegrationTest
    extends FunSpec
    with Matchers
    with Messaging
    with Eventually
    with ExtendedPatience {

  it("sends and receives messages") {
    withMessageWorkerFixtures {
      case (_, queue, metrics, bucket, worker) =>
        withLocalSnsTopic { topic =>
          subscribeTopicToQueue(queue, topic)

          val s3Config = S3Config(bucketName = bucket.name)

          val snsConfig = SNSConfig(topic.arn)
          val snsWriter = new SNSWriter(snsClient, snsConfig)

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
