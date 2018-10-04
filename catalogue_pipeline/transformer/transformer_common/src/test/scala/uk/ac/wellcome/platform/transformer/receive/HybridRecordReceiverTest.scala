package uk.ac.wellcome.platform.transformer.receive

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.message.{MessageWriter, MessageWriterConfig}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.work.internal.{
  TransformedBaseWork,
  UnidentifiedWork
}
import uk.ac.wellcome.models.work.test.util.WorksGenerators
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException
import uk.ac.wellcome.platform.transformer.utils.HybridRecordMessageGenerator
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

class HybridRecordReceiverTest
    extends FunSpec
    with Matchers
    with SQS
    with SNS
    with S3
    with Messaging
    with Eventually
    with IntegrationPatience
    with MockitoSugar
    with ScalaFutures
    with HybridRecordMessageGenerator
    with WorksGenerators {

  case class TestException(message: String) extends Exception(message)
  case class TestTransformable()
  def transformToWork(transforrmable: TestTransformable, version: Int) =
    Try(createUnidentifiedWorkWith(version = version))
  def failingTransformToWork(transforrmable: TestTransformable, version: Int) =
    Try(throw TestException("BOOOM!"))

  it("receives a message and sends it to SNS client") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { _ =>
        withLocalS3Bucket { bucket =>
          val sqsMessage = hybridRecordNotificationMessage(
            message = toJson(TestTransformable()).get,
            s3Client = s3Client,
            bucket = bucket)

          withHybridRecordReceiver(topic, bucket) { recordReceiver =>
            val future =
              recordReceiver.receiveMessage(sqsMessage, transformToWork)

            whenReady(future) { _ =>
              val works = getMessages[TransformedBaseWork](topic)
              works.size should be >= 1

              works.map { work =>
                work shouldBe a[UnidentifiedWork]
              }
            }
          }
        }
      }
    }
  }

  it("receives a message and adds the version to the transformed work") {
    val version = 5

    withLocalSnsTopic { topic =>
      withLocalSqsQueue { _ =>
        withLocalS3Bucket { bucket =>
          val sierraMessage = hybridRecordNotificationMessage(
            message = toJson(TestTransformable()).get,
            version = version,
            s3Client = s3Client,
            bucket = bucket)

          withHybridRecordReceiver(topic, bucket) { recordReceiver =>
            val future =
              recordReceiver.receiveMessage(sierraMessage, transformToWork)

            whenReady(future) { _ =>
              val works = getMessages[TransformedBaseWork](topic)
              works.size should be >= 1

              works.map { actualWork =>
                actualWork shouldBe a[UnidentifiedWork]
                val unidentifiedWork = actualWork.asInstanceOf[UnidentifiedWork]
                unidentifiedWork.version shouldBe version
              }
            }
          }
        }
      }
    }
  }

  it("returns a failed future if it's unable to parse the SQS message") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { _ =>
        withLocalS3Bucket { bucket =>
          val invalidSqsMessage =
            hybridRecordNotificationMessage(
              message = "not a json string",
              s3Client = s3Client,
              bucket = bucket)

          withHybridRecordReceiver(topic, bucket) { recordReceiver =>
            val future =
              recordReceiver.receiveMessage(invalidSqsMessage, transformToWork)

            whenReady(future.failed) { x =>
              x shouldBe a[TransformerException]
            }
          }
        }
      }
    }
  }

  it("fails if it's unable to perform a transformation") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { _ =>
        withLocalS3Bucket { bucket =>
          val failingSqsMessage =
            hybridRecordNotificationMessage(
              message = toJson(TestTransformable).get,
              s3Client = s3Client,
              bucket = bucket)

          withHybridRecordReceiver(topic, bucket) { recordReceiver =>
            val future =
              recordReceiver.receiveMessage(
                failingSqsMessage,
                failingTransformToWork)

            whenReady(future.failed) { x =>
              x shouldBe a[TestException]
            }
          }
        }
      }
    }
  }

  it("fails if it's unable to publish the work") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { _ =>
        withLocalS3Bucket { bucket =>
          val message = hybridRecordNotificationMessage(
            message = toJson(TestTransformable).get,
            s3Client = s3Client,
            bucket = bucket)

          withHybridRecordReceiver(
            topic,
            bucket,
            Some(mockSnsClientFailPublishMessage)) { recordReceiver =>
            val future = recordReceiver.receiveMessage(message, transformToWork)

            whenReady(future.failed) { x =>
              x.getMessage should be("Failed publishing message")
            }
          }
        }
      }
    }
  }

  def withHybridRecordReceiver[R](
    topic: Topic,
    bucket: Bucket,
    maybeSnsClient: Option[AmazonSNS] = None
  )(testWith: TestWith[HybridRecordReceiver[TestTransformable], R])(
    implicit objectStore: ObjectStore[TestTransformable]) = {
    val s3Config = S3Config(bucket.name)

    val messageConfig = MessageWriterConfig(SNSConfig(topic.arn), s3Config)

    val messageWriter =
      new MessageWriter[TransformedBaseWork](
        messageConfig = messageConfig,
        snsClient = maybeSnsClient.getOrElse(snsClient),
        s3Client = s3Client
      )

    val recordReceiver = new HybridRecordReceiver[TestTransformable](
      messageWriter = messageWriter,
      objectsStore = objectStore
    )

    testWith(recordReceiver)
  }

  private def mockSnsClientFailPublishMessage = {
    val mockSNSClient = mock[AmazonSNS]
    when(mockSNSClient.publish(any[PublishRequest]))
      .thenThrow(new RuntimeException("Failed publishing message"))
    mockSNSClient
  }
}
