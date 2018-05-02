package uk.ac.wellcome.transformer.receive

import java.time.Instant

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.message.{MessageConfig, MessageWriter}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSMessage
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.models.transformable.{SierraTransformable, Transformable}
import uk.ac.wellcome.models.work.internal.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.transformer.modules.UnidentifiedWorkKeyPrefixGenerator
import uk.ac.wellcome.transformer.utils.TransformableMessageUtils
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.duration._

class SQSMessageReceiverTest
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
      identifierScheme = IdentifierSchemes.calmPlaceholder,
      ontologyType = "Work",
      value = "value")

  val work = UnidentifiedWork(
    title = Some("placeholder title"),
    sourceIdentifier = sourceIdentifier,
    version = 1,
    identifiers = List(sourceIdentifier)
  )

  val metricsSender: MetricsSender = new MetricsSender(
    namespace = "record-receiver-tests",
    100 milliseconds,
    mock[AmazonCloudWatch],
    ActorSystem()
  )

  def withSQSMessageReceiver[R](
                                 topic: Topic,
                                 bucket: Bucket,
                                 maybeSnsClient: Option[AmazonSNS] = None
                               )(testWith: TestWith[SQSMessageReceiver, R]) = {

    val s3Config = S3Config(bucket.name)

    val messageConfig = MessageConfig(SNSConfig(topic.arn), s3Config)

    val messageWriter =
      new MessageWriter[UnidentifiedWork](messageConfig, maybeSnsClient.getOrElse(snsClient), s3Client, new UnidentifiedWorkKeyPrefixGenerator())

    val recordReceiver = new SQSMessageReceiver(
      messageWriter = messageWriter,
      s3Client = s3Client,
      s3Config = S3Config(bucket.name),
      metricsSender = metricsSender
    )

    testWith(recordReceiver)
  }

  it("receives a message and sends it to SNS client") {

    withLocalSnsTopic { topic =>
      withLocalSqsQueue { _ =>
        withLocalS3Bucket { bucket =>
          val calmSqsMessage: SQSMessage = hybridRecordSqsMessage(
            message = createValidCalmTramsformableJson(
              RecordID = "abcdef",
              RecordType = "collection",
              AltRefNo = "AB/CD/12",
              RefNo = "AB/CD/12",
              data = """{"foo": ["bar"], "AccessStatus": ["restricted"]}"""
            ),
            sourceName = "calm",
            version = 1,
            s3Client = s3Client,
            bucket = bucket
          )

          withSQSMessageReceiver(topic, bucket) { recordReceiver =>
            val future = recordReceiver.receiveMessage(calmSqsMessage)

            whenReady(future) { _ =>
              val snsMessages = listMessagesReceivedFromSNS(topic)
              snsMessages.size should be >= 1

              snsMessages.map { snsMessage =>
                get[UnidentifiedWork](snsMessage)
                snsMessage.subject shouldBe "source: SQSMessageReceiver.publishMessage"
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
          val sierraMessage: SQSMessage = hybridRecordSqsMessage(
            message =
              createValidSierraTransformableJson(id, title, lastModifiedDate),
            sourceName = "sierra",
            version = version,
            s3Client = s3Client,
            bucket = bucket
          )

          withSQSMessageReceiver(topic, bucket) { recordReceiver =>
            val future = recordReceiver.receiveMessage(sierraMessage)

            val sourceIdentifier = SourceIdentifier(
              identifierScheme = IdentifierSchemes.sierraSystemNumber,
              ontologyType = "Work",
              value = "b50050059"
            )
            val sierraIdentifier = SourceIdentifier(
              identifierScheme = IdentifierSchemes.sierraIdentifier,
              ontologyType = "Work",
              value = id
            )

            whenReady(future) { _ =>
              val snsMessages = listMessagesReceivedFromSNS(topic)
              snsMessages.size should be >= 1

              snsMessages.map { snsMessage =>
                val actualWork = get[UnidentifiedWork](snsMessage)

                actualWork.title shouldBe Some(title)
                actualWork.sourceIdentifier shouldBe sourceIdentifier
                actualWork.version shouldBe version
                actualWork.identifiers shouldBe List(
                  sourceIdentifier,
                  sierraIdentifier)

                snsMessage.subject shouldBe "source: SQSMessageReceiver.publishMessage"
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
          val invalidCalmSqsMessage: SQSMessage =
            hybridRecordSqsMessage(
              message = "not a json string",
              sourceName = "calm",
              version = 1,
              s3Client = s3Client,
              bucket = bucket
            )

          withSQSMessageReceiver(topic, bucket) { recordReceiver =>
            val future = recordReceiver.receiveMessage(invalidCalmSqsMessage)

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
          withSQSMessageReceiver(topic, bucket) {
            recordReceiver =>
              val future = recordReceiver.receiveMessage(
                createValidEmptySierraBibSQSMessage(
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
          val failingTransformCalmSqsMessage: SQSMessage =
            hybridRecordSqsMessage(
              message = createValidCalmTramsformableJson(
                RecordID = "abcdef",
                RecordType = "collection",
                AltRefNo = "AB/CD/12",
                RefNo = "AB/CD/12",
                data = """not a json string"""
              ),
              sourceName = "calm",
              version = 1,
              s3Client = s3Client,
              bucket = bucket
            )

          withSQSMessageReceiver(topic, bucket) { recordReceiver =>
            val future =
              recordReceiver.receiveMessage(failingTransformCalmSqsMessage)

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
              data = s"""{"id": "$id"}""",
              modifiedDate = Instant.now))
          .get)

    withLocalSnsTopic { topic =>
      withLocalSqsQueue { _ =>
        withLocalS3Bucket { bucket =>
          val message = hybridRecordSqsMessage(
            message = JsonUtil.toJson(sierraTransformable).get,
            sourceName = "sierra",
            version = 1,
            s3Client = s3Client,
            bucket = bucket
          )

          withSQSMessageReceiver(topic, bucket, Some(mockSnsClientFailPublishMessage)) {
            recordReceiver =>
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
