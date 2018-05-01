package uk.ac.wellcome.messaging.message

import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.utils.JsonUtil._
import org.scalatest.{FunSpec, Matchers}
import org.mockito.Matchers.{any, matches}
import org.mockito.Mockito.{times, verify}
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.messaging.sns.{SNSConfig, SNSWriter}
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSReader}
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.models.aws.S3Config
import uk.ac.wellcome.s3.S3ObjectStore
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.duration._

class MessagingIntegrationTest
    extends FunSpec
    with Matchers
    with Messaging
    with Eventually
    with ExtendedPatience {

  val message = ExampleObject("A message sent in the MessagingIntegrationTest")
  val subject = "message-integration-test-subject"

  it("sends and receives messages") {
    withLocalStackMessageWriter { case (worker, messageWriter) =>
      messageWriter.write(message = message, subject = subject)

      eventually {
        worker.calledWith shouldBe Some(message)
      }
    }
  }

  private def withLocalStackMessageWriter[R](testWith: TestWith[(ExampleMessageWorker, MessageWriter[ExampleObject]), R]) = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withLocalStackSqsQueue { queue =>

              withLocalS3Bucket { bucket =>
                val s3Config = S3Config(bucketName = bucket.name)
                val snsConfig = SNSConfig(topicArn = topic.arn)

                val messageConfig = MessageConfig(
                  s3Config = s3Config,
                  snsConfig = snsConfig
                )

                val messageReader = new MessageReader[ExampleObject](
                  messageConfig = messageConfig,
                  s3Client = s3Client,
                  keyPrefixGenerator = keyPrefixGenerator
                )

                val sqsReader = new SQSReader(
                  sqsClient = localStackSqsClient,
                  sqsConfig = SQSConfig(queue.url, 1.second, 1)
                )

                val worker = new ExampleMessageWorker(
                  sqsReader = sqsReader,
                  messageReader = messageReader,
                  actorSystem = actorSystem,
                  metricsSender = metricsSender
                )

                withMessageWriter(bucket, topic, localStackSnsClient) { messageWriter =>
                  testWith((worker, messageWriter))
                }
              }
            }
          }
        }
      }
    }
  }
}
