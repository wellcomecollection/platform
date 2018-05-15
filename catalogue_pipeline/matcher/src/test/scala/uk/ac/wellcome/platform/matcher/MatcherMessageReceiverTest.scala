package uk.ac.wellcome.platform.matcher

import com.amazonaws.services.s3.AmazonS3
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig, SNSWriter}
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSStream}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.duration._

class MatcherMessageReceiverTest extends FunSpec
  with Matchers
  with Akka
  with SQS
  with SNS
  with S3
  with Messaging
  with ExtendedPatience
  with Eventually {


  def withMatcherMessageReceiver[R](queue: SQS.Queue,
                                    storageBucket: Bucket,
                                    topic: Topic)
                                   (testWith: TestWith[MatcherMessageReceiver, R]) = {
    val storageS3Config = S3Config(storageBucket.name)

    val snsWriter =
      new SNSWriter(
        snsClient,
        SNSConfig(topic.arn))


    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        val sqsStream = new SQSStream[NotificationMessage](
          actorSystem = actorSystem,
          sqsClient = asyncSqsClient,
          sqsConfig = SQSConfig(queue.url, 1 second, 1),
          metricsSender = metricsSender
        )
        val matcherMessageReceiver = new MatcherMessageReceiver(sqsStream, snsWriter, s3Client, storageS3Config, actorSystem)
        testWith(matcherMessageReceiver)
      }
    }
  }

  it("receives a message with UnidentifiedWork") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
          withLocalS3Bucket { storageBucket =>
            val work = UnidentifiedWork(
              sourceIdentifier = SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "Work", "id"),
              title = Some("Work"),
              version = 1
            )
            val workSqsMessage: NotificationMessage = hybridRecordNotificationMessage(
              message = toJson(RecorderWorkEntry(
                work = work)).get,
              version = 1,
              s3Client = s3Client,
              bucket = storageBucket
            )
            sqsClient.sendMessage(
              queue.url,
              toJson(workSqsMessage).get
            )

            withMatcherMessageReceiver(queue, storageBucket, topic) { _ =>
              eventually {
                val snsMessages = listMessagesReceivedFromSNS(topic)
                snsMessages.size should be >= 1

                snsMessages.map { snsMessage =>
                  val redirectList = fromJson[RedirectList](snsMessage.message).get
                  redirectList shouldBe RedirectList(List(Redirect(target = work.sourceIdentifier, sources = List())))
                }
              }
            }
        }
      }
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
}
