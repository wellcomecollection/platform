package uk.ac.wellcome.platform.matcher

import com.amazonaws.services.s3.AmazonS3
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig, SNSWriter}
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSStream}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{
  IdentifierSchemes,
  SourceIdentifier,
  UnidentifiedWork
}
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.s3.{S3Config, S3StringStore, S3TypeStore}
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.duration._

class MatcherMessageReceiverTest
    extends FunSpec
    with Matchers
    with Akka
    with SQS
    with SNS
    with S3
    with MetricsSenderFixture
    with ExtendedPatience
    with Eventually {

  def withMatcherMessageReceiver[R](
    queue: SQS.Queue,
    storageBucket: Bucket,
    topic: Topic)(testWith: TestWith[MatcherMessageReceiver, R]) = {
    val storageS3Config = S3Config(storageBucket.name)

    val snsWriter =
      new SNSWriter(snsClient, SNSConfig(topic.arn))

    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        val sqsStream = new SQSStream[NotificationMessage](
          actorSystem = actorSystem,
          sqsClient = asyncSqsClient,
          sqsConfig = SQSConfig(queue.url, 1 second, 1),
          metricsSender = metricsSender
        )
        val matcherMessageReceiver = new MatcherMessageReceiver(
          sqsStream,
          snsWriter,
          new S3TypeStore[RecorderWorkEntry](
            new S3StringStore(s3Client, storageS3Config)),
          storageS3Config,
          actorSystem)
        testWith(matcherMessageReceiver)
      }
    }
  }

  it("sends no redirects for a work without identifiers") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          val work = unidentifiedWork
          sendSQS(queue, storageBucket, work)

          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            eventually {
              val snsMessages = listMessagesReceivedFromSNS(topic)
              snsMessages.size should be >= 1

              snsMessages.map { snsMessage =>
                val redirectList =
                  fromJson[RedirectList](snsMessage.message).get
                redirectList shouldBe RedirectList(List(
                  Redirect(target = work.sourceIdentifier, sources = List())))
              }
            }
          }
        }
      }
    }
  }

  it(
    "sends a redirect to the component and combined works for a work with one identifier") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          val sourceIdentifier = SourceIdentifier(
            IdentifierSchemes.sierraSystemNumber,
            "Work",
            "editedWork")
          val linkedIdentifier = SourceIdentifier(
            IdentifierSchemes.sierraSystemNumber,
            "Work",
            "linkedWork")
          val work = unidentifiedWork.copy(
            sourceIdentifier = sourceIdentifier,
            identifiers = List(sourceIdentifier, linkedIdentifier))

          sendSQS(queue, storageBucket, work)

          withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
            eventually {
              val snsMessages = listMessagesReceivedFromSNS(topic)
              snsMessages.size should be >= 1

              snsMessages.map { snsMessage =>
                val redirectList =
                  fromJson[RedirectList](snsMessage.message).get

                val combinedIdentifier =
                  SourceIdentifier(
                    IdentifierSchemes.mergedWork,
                    "Work",
                    "sierra-system-number/editedWork+sierra-system-number/linkedWork")

                redirectList shouldBe RedirectList(
                  List(Redirect(
                    target = combinedIdentifier,
                    sources = List(sourceIdentifier, linkedIdentifier))))
              }
            }
          }
        }
      }
    }
  }

  private def sendSQS(queue: SQS.Queue,
                      storageBucket: Bucket,
                      work: UnidentifiedWork) = {
    val workSqsMessage: NotificationMessage =
      hybridRecordNotificationMessage(
        message = toJson(RecorderWorkEntry(work = work)).get,
        version = 1,
        s3Client = s3Client,
        bucket = storageBucket
      )
    sqsClient.sendMessage(
      queue.url,
      toJson(workSqsMessage).get
    )
  }

  private def unidentifiedWork = {
    val sourceIdentifier =
      SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "Work", "id")
    UnidentifiedWork(
      sourceIdentifier = sourceIdentifier,
      title = Some("Work"),
      version = 1,
      identifiers = List(sourceIdentifier)
    )
  }

  def hybridRecordNotificationMessage(message: String,
                                      version: Int,
                                      s3Client: AmazonS3,
                                      bucket: Bucket) = {
    val key = "recorder/1/testId/dshg548.json"
    s3Client.putObject(bucket.name, key, message)

    val hybridRecord = HybridRecord(
      id = "testId",
      version = version,
      s3key = key
    )

    NotificationMessage(
      "messageId",
      "topicArn",
      "subject",
      toJson(hybridRecord).get
    )
  }

}
