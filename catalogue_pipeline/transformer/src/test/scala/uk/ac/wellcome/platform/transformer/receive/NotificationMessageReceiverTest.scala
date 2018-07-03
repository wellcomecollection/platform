package uk.ac.wellcome.platform.transformer.receive

import java.time.Instant

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
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.models.transformable.{MiroTransformable, SierraTransformable, Transformable}
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier, TransformedBaseWork, UnidentifiedWork}
import uk.ac.wellcome.storage.s3.{S3Config, S3StorageBackend}
import uk.ac.wellcome.platform.transformer.utils.TransformableMessageUtils
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
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
    with TransformableMessageUtils {

  val sourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType("calm-altref-no"),
      ontologyType = "Work",
      value = "value")

  val work = UnidentifiedWork(
    title = "placeholder title",
    sourceIdentifier = sourceIdentifier,
    version = 1
  )

  def withNotificationMessageReceiver[R](
    topic: Topic,
    bucket: Bucket,
    maybeSnsClient: Option[AmazonSNS] = None
  )(testWith: TestWith[NotificationMessageReceiver, R]) = {
    val s3Config = S3Config(bucket.name)

    val messageConfig = MessageWriterConfig(SNSConfig(topic.arn), s3Config)

    // Required for MessageWriter
    implicit val storageBackend = new S3StorageBackend(s3Client)

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
              id = "1234567",
              title = "A calming breeze on the sea",
              lastModifiedDate = Instant.now
            ),
            sourceName = "sierra",
            version = 1,
            s3Client = s3Client,
            bucket = bucket
          )

          withNotificationMessageReceiver(topic, bucket) { recordReceiver =>
            val future = recordReceiver.receiveMessage(sqsMessage)

            whenReady(future) { _ =>
              val snsMessages = listMessagesReceivedFromSNS(topic)
              snsMessages.size should be >= 1

              snsMessages.map { snsMessage =>
                get[UnidentifiedWork](snsMessage)
                snsMessage.subject shouldBe "source: NotificationMessageReceiver.publishMessage"
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
    val lastModifiedDate = Instant.now()
    val version = 5

    withLocalSnsTopic { topic =>
      withLocalSqsQueue { _ =>
        withLocalS3Bucket { bucket =>
          val sierraMessage = hybridRecordNotificationMessage(
            message =
              createValidSierraTransformableJson(id, title, lastModifiedDate),
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
              val snsMessages = listMessagesReceivedFromSNS(topic)
              snsMessages.size should be >= 1

              snsMessages.map { snsMessage =>
                val actualWork = get[UnidentifiedWork](snsMessage)

                actualWork.title shouldBe title
                actualWork.sourceIdentifier shouldBe sourceIdentifier
                actualWork.version shouldBe version
                actualWork.identifiers shouldBe List(
                  sourceIdentifier,
                  sierraIdentifier)

                snsMessage.subject shouldBe "source: NotificationMessageReceiver.publishMessage"
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

  it("sends no message where Transformable work is None") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { _ =>
        withLocalS3Bucket { bucket =>
          withNotificationMessageReceiver(topic, bucket) { recordReceiver =>
            val future = recordReceiver.receiveMessage(
              createValidEmptySierraBibNotificationMessage(
                id = "0101010",
                s3Client = s3Client,
                bucket = bucket
              )
            )

            whenReady(future) { _ =>
              val snsMessages = listMessagesReceivedFromSNS(topic)
              snsMessages shouldBe empty
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
    val id = "1001001"
    val sierraTransformable: Transformable =
      SierraTransformable(
        sourceId = id,
        bibData = JsonUtil
          .toJson(
            SierraBibRecord(
              id = id,
              data = s"""{"id": "$id", "title": "A title"}""",
              modifiedDate = Instant.now))
          .get)

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
