package uk.ac.wellcome.transformer.receive

import java.time.Instant

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SNSConfig, SQSMessage}
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.models.transformable.{SierraTransformable, Transformable}
import uk.ac.wellcome.models.{
  IdentifierSchemes,
  SourceIdentifier,
  UnidentifiedWork
}
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.test.fixtures.{S3, SnsFixtures, SqsFixtures, TestWith}
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
    with SqsFixtures
    with SnsFixtures
    with S3
    with Eventually
    with ExtendedPatience
    with MockitoSugar
    with ScalaFutures
    with TransformableMessageUtils {

  val sourceIdentifier =
    SourceIdentifier(IdentifierSchemes.calmPlaceholder, "value")

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
    topicArn: String,
    bucketName: String,
    maybeSnsWriter: Option[SNSWriter] = None
  )(testWith: TestWith[SQSMessageReceiver, R]) = {

    val snsWriter =
      maybeSnsWriter.getOrElse(new SNSWriter(snsClient, SNSConfig(topicArn)))
    val recordReceiver =
      new SQSMessageReceiver(snsWriter, s3Client, bucketName, metricsSender)

    testWith(recordReceiver)
  }

  it("receives a message and send it to SNS client") {

    withLocalSnsTopic { topicArn =>
      withLocalSqsQueue { queueUrl =>
        withLocalS3Bucket { bucketName =>
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
            bucketName = bucketName
          )

          withSQSMessageReceiver(topicArn, bucketName) { recordReceiver =>
            val future = recordReceiver.receiveMessage(calmSqsMessage)

            whenReady(future) { _ =>
              val messages = listMessagesReceivedFromSNS(topicArn)
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

    withLocalSnsTopic { topicArn =>
      withLocalSqsQueue { queueUrl =>
        withLocalS3Bucket { bucketName =>
          val sierraMessage: SQSMessage = hybridRecordSqsMessage(
            message =
              createValidSierraTransformableJson(id, title, lastModifiedDate),
            sourceName = "sierra",
            version = version,
            s3Client = s3Client,
            bucketName = bucketName
          )

          withSQSMessageReceiver(topicArn, bucketName) { recordReceiver =>
            val future = recordReceiver.receiveMessage(sierraMessage)

            whenReady(future) { _ =>
              val messages = listMessagesReceivedFromSNS(topicArn)
              messages should have size 1
              messages.head.message shouldBe JsonUtil
                .toJson(UnidentifiedWork(
                  title = Some(title),
                  sourceIdentifier =
                    SourceIdentifier(IdentifierSchemes.sierraSystemNumber, id),
                  version = version,
                  identifiers = List(
                    SourceIdentifier(IdentifierSchemes.sierraSystemNumber, id))
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
    withLocalSnsTopic { topicArn =>
      withLocalSqsQueue { queueUrl =>
        withLocalS3Bucket { bucketName =>
          val invalidCalmSqsMessage: SQSMessage =
            hybridRecordSqsMessage(
              message = "not a json string",
              sourceName = "calm",
              version = 1,
              s3Client = s3Client,
              bucketName = bucketName
            )

          withSQSMessageReceiver(topicArn, bucketName) { recordReceiver =>
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
    withLocalSnsTopic { topicArn =>
      withLocalSqsQueue { queueUrl =>
        withLocalS3Bucket { bucketName =>
          val snsWriter = mockSNSWriter

          withSQSMessageReceiver(topicArn, bucketName, Some(snsWriter)) {
            recordReceiver =>
              val future = recordReceiver.receiveMessage(
                createValidEmptySierraBibSQSMessage(
                  id = "000",
                  s3Client = s3Client,
                  bucketName = bucketName
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
    withLocalSnsTopic { topicArn =>
      withLocalSqsQueue { queueUrl =>
        withLocalS3Bucket { bucketName =>
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
              bucketName = bucketName
            )

          withSQSMessageReceiver(topicArn, bucketName) { recordReceiver =>
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

    withLocalSnsTopic { topicArn =>
      withLocalSqsQueue { queueUrl =>
        withLocalS3Bucket { bucketName =>
          val message = hybridRecordSqsMessage(
            message = JsonUtil.toJson(sierraTransformable).get,
            sourceName = "sierra",
            version = 1,
            s3Client = s3Client,
            bucketName = bucketName
          )

          val snsWriter = mockFailPublishMessage

          withSQSMessageReceiver(topicArn, bucketName, Some(snsWriter)) {
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
