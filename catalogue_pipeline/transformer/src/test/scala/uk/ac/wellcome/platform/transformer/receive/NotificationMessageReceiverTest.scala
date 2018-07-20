package uk.ac.wellcome.platform.transformer.receive

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.message.{MessageWriter, MessageWriterConfig}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.models.transformable.{
  MiroTransformable,
  SierraTransformable
}
import uk.ac.wellcome.models.transformable.SierraTransformableCodec._
import uk.ac.wellcome.models.work.internal.{
  IdentifierType,
  SourceIdentifier,
  TransformedBaseWork,
  UnidentifiedWork
}
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.{S3Config, S3StorageBackend}
import uk.ac.wellcome.platform.transformer.utils.TransformableMessageUtils
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class NotificationMessageReceiverTest
    extends FunSpec
    with Matchers
    with SQS
    with SNS
    with S3
    with Messaging
    with Eventually
    with ExtendedPatience
    with MockitoSugar
    with ScalaFutures
    with SierraUtil
    with TransformableMessageUtils {

  def withNotificationMessageReceiver[R](
    topic: Topic,
    bucket: Bucket,
    maybeSnsClient: Option[AmazonSNS] = None
  )(testWith: TestWith[NotificationMessageReceiver, R]) = {
    val s3Config = S3Config(bucket.name)

    val messageConfig = MessageWriterConfig(SNSConfig(topic.arn), s3Config)

    // Required for MessageWriter
    implicit val storageBackend = new S3StorageBackend(s3Client)

    implicit val miroTransformableStore = ObjectStore[MiroTransformable]
    implicit val sierraTransformableStore = ObjectStore[SierraTransformable]

    val messageWriter =
      new MessageWriter[TransformedBaseWork](
        messageConfig = messageConfig,
        snsClient = maybeSnsClient.getOrElse(snsClient),
        s3Client = s3Client
      )

    val recordReceiver = new NotificationMessageReceiver(
      messageWriter = messageWriter,
      s3Client = s3Client,
      s3Config = S3Config(bucket.name)
    )

    testWith(recordReceiver)
  }

  it("receives a message and sends it to SNS client") {

    withLocalSnsTopic { topic =>
      withLocalSqsQueue { _ =>
        withLocalS3Bucket { bucket =>
          val sqsMessage = hybridRecordNotificationMessage(
            message = createValidSierraTransformableJson(
              id = createSierraRecordNumberString,
              title = "A calming breeze on the sea"),
            sourceName = "sierra",
            version = 1,
            s3Client = s3Client,
            bucket = bucket
          )

          withNotificationMessageReceiver(topic, bucket) { recordReceiver =>
            val future = recordReceiver.receiveMessage(sqsMessage)

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

  it("receives a message and add the version to the transformed work") {
    val id = "5005005"
    val title = "A pot of possums"
    val version = 5

    withLocalSnsTopic { topic =>
      withLocalSqsQueue { _ =>
        withLocalS3Bucket { bucket =>
          val sierraMessage = hybridRecordNotificationMessage(
            message = createValidSierraTransformableJson(id, title),
            sourceName = "sierra",
            version = version,
            s3Client = s3Client,
            bucket = bucket
          )

          withNotificationMessageReceiver(topic, bucket) { recordReceiver =>
            val future = recordReceiver.receiveMessage(sierraMessage)

            val sourceIdentifier = SourceIdentifier(
              identifierType = IdentifierType("sierra-system-number"),
              ontologyType = "Work",
              value = "b50050059"
            )
            val sierraIdentifier = SourceIdentifier(
              identifierType = IdentifierType("sierra-identifier"),
              ontologyType = "Work",
              value = id
            )

            whenReady(future) { _ =>
              val works = getMessages[TransformedBaseWork](topic)
              works.size should be >= 1

              works.map { actualWork =>
                actualWork shouldBe a[UnidentifiedWork]
                val unidentifiedWork = actualWork.asInstanceOf[UnidentifiedWork]

                unidentifiedWork.title shouldBe title
                unidentifiedWork.sourceIdentifier shouldBe sourceIdentifier
                unidentifiedWork.version shouldBe version
                unidentifiedWork.identifiers shouldBe List(
                  sourceIdentifier,
                  sierraIdentifier)
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
              sourceName = "miro",
              version = 1,
              s3Client = s3Client,
              bucket = bucket
            )

          withNotificationMessageReceiver(topic, bucket) { recordReceiver =>
            val future = recordReceiver.receiveMessage(invalidSqsMessage)

            whenReady(future.failed) { x =>
              x shouldBe a[GracefulFailureException]
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
          val miroTransformable = MiroTransformable(
            sourceId = "B1234567",
            MiroCollection = "Images-B",
            data = "not a json string"
          )

          val failingSqsMessage =
            hybridRecordNotificationMessage(
              message = toJson(miroTransformable).get,
              sourceName = "miro",
              s3Client = s3Client,
              bucket = bucket
            )

          withNotificationMessageReceiver(topic, bucket) { recordReceiver =>
            val future =
              recordReceiver.receiveMessage(failingSqsMessage)

            whenReady(future.failed) { x =>
              x shouldBe a[GracefulFailureException]
            }
          }
        }
      }
    }
  }

  it("fails if it's unable to publish the work") {
    val sierraTransformable = SierraTransformable(
      bibRecord = createSierraBibRecord
    )

    withLocalSnsTopic { topic =>
      withLocalSqsQueue { _ =>
        withLocalS3Bucket { bucket =>
          val message = hybridRecordNotificationMessage(
            message = JsonUtil.toJson(sierraTransformable).get,
            sourceName = "sierra",
            version = 1,
            s3Client = s3Client,
            bucket = bucket
          )

          withNotificationMessageReceiver(
            topic,
            bucket,
            Some(mockSnsClientFailPublishMessage)) { recordReceiver =>
            val future = recordReceiver.receiveMessage(message)

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
