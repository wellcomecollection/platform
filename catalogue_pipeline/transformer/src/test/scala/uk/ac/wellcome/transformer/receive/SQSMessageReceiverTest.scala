package uk.ac.wellcome.transformer.receive

import java.time.Instant

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.metrics.MetricsSender
import uk.ac.wellcome.messaging.sns.{PublishAttempt, SNSConfig, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSMessage
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.models.transformable.{SierraTransformable, Transformable}
import uk.ac.wellcome.models.work.internal.{
  IdentifierSchemes,
  SourceIdentifier,
  UnidentifiedWork
}
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.transformer.utils.TransformableMessageUtils
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.concurrent.duration._

class SQSMessageReceiverTest
    extends FunSpec
    with Matchers
    with SQS
    with SNS
    with S3
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
    maybeSnsWriter: Option[SNSWriter] = None
  )(testWith: TestWith[SQSMessageReceiver, R]) = {

    val snsWriter =
      maybeSnsWriter.getOrElse(new SNSWriter(snsClient, SNSConfig(topic.arn)))
    val recordReceiver = new SQSMessageReceiver(
      snsWriter = snsWriter,
      s3Client = s3Client,
      s3Config = S3Config(bucket.name),
      metricsSender = metricsSender
    )

    testWith(recordReceiver)
  }

  it("receives a message and send it to SNS client") {

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
              val messages = listMessagesReceivedFromSNS(topic)
              messages should have size 1
              messages.head.message shouldBe JsonUtil.toJson(work).get
              messages.head.subject shouldBe "source: SQSMessageReceiver.publishMessage"
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
              val messages = listMessagesReceivedFromSNS(topic)
              messages should have size 1
              messages.head.message shouldBe JsonUtil
                .toJson(
                  UnidentifiedWork(
                    title = Some(title),
                    sourceIdentifier = sourceIdentifier,
                    version = version,
                    identifiers = List(sourceIdentifier, sierraIdentifier)
                  ))
                .get
              messages.head.subject shouldBe "source: SQSMessageReceiver.publishMessage"
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
          val snsWriter = mockSNSWriter

          withSQSMessageReceiver(topic, bucket, Some(snsWriter)) {
            recordReceiver =>
              val future = recordReceiver.receiveMessage(
                createValidEmptySierraBibSQSMessage(
                  id = "0101010",
                  s3Client = s3Client,
                  bucket = bucket
                )
              )

              whenReady(future) { x =>
                verify(snsWriter, Mockito.never())
                  .writeMessage(anyString, any[String])
              }
          }
        }
      }
    }
  }

  it(
    "returns a failed future if it's unable to transform the transformable object") {
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

  it("should return a failed future if it's unable to publish the work") {

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

          val snsWriter = mockFailPublishMessage

          withSQSMessageReceiver(topic, bucket, Some(snsWriter)) {
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

  private def mockSNSWriter = {
    val mockSNS = mock[SNSWriter]
    when(mockSNS.writeMessage(anyString(), any[String]))
      .thenReturn(Future { PublishAttempt(Right("1234")) })
    mockSNS
  }

  private def mockFailPublishMessage = {
    val mockSNS = mock[SNSWriter]
    when(mockSNS.writeMessage(anyString(), any[String]))
      .thenReturn(
        Future.failed(new RuntimeException("Failed publishing message")))
    mockSNS
  }
}
