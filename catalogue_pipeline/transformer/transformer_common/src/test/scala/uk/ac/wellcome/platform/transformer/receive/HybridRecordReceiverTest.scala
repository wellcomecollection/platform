package uk.ac.wellcome.platform.transformer.receive

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.{TransformedBaseWork, UnidentifiedWork}
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException
import uk.ac.wellcome.platform.transformer.fixtures.HybridRecordReceiverFixture
import uk.ac.wellcome.storage.{ObjectLocation, ObjectStore}
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord
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
    with HybridRecordReceiverFixture
    with IntegrationPatience
    with MockitoSugar
    with ScalaFutures
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
          val sqsMessage = createHybridRecordNotificationWith(
            TestTransformable(),
            s3Client = s3Client,
            bucket = bucket
          )

          withHybridRecordReceiver[TestTransformable, List[Assertion]](topic, bucket) { recordReceiver =>
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
          val notification = createHybridRecordNotificationWith(
            TestTransformable(),
            version = version,
            s3Client = s3Client,
            bucket = bucket
          )

          withHybridRecordReceiver[TestTransformable, Assertion](topic, bucket) { recordReceiver =>
            val future =
              recordReceiver.receiveMessage(notification, transformToWork)

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
          val key = randomAlphanumeric(10)
          s3Client.putObject(bucket.name, key, "not a JSON string")

          val hybridRecord = HybridRecord(
            id = "testId",
            version = 1,
            location = ObjectLocation(namespace = bucket.name, key = key)
          )
          val invalidSqsMessage = createNotificationMessageWith(
            message = hybridRecord
          )

          withHybridRecordReceiver[TestTransformable, Assertion](topic, bucket) { recordReceiver =>
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
          val failingSqsMessage = createHybridRecordNotificationWith(
            TestTransformable(),
            s3Client = s3Client,
            bucket = bucket
          )

          withHybridRecordReceiver[TestTransformable, Assertion](topic, bucket) { recordReceiver =>
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
          val message = createHybridRecordNotificationWith(
            TestTransformable(),
            s3Client = s3Client,
            bucket = bucket
          )

          withHybridRecordReceiver[TestTransformable, Assertion](
            topic,
            bucket,
            mockSnsClientFailPublishMessage) { recordReceiver =>
            val future = recordReceiver.receiveMessage(message, transformToWork)

            whenReady(future.failed) { x =>
              x.getMessage should be("Failed publishing message")
            }
          }
        }
      }
    }
  }

  private def mockSnsClientFailPublishMessage = {
    val mockSNSClient = mock[AmazonSNS]
    when(mockSNSClient.publish(any[PublishRequest]))
      .thenThrow(new RuntimeException("Failed publishing message"))
    mockSNSClient
  }
}
