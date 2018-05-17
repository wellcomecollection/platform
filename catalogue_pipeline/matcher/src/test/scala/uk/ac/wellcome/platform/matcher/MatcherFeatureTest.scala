package uk.ac.wellcome.platform.matcher

import com.amazonaws.services.s3.AmazonS3
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{
  IdentifierSchemes,
  SourceIdentifier,
  UnidentifiedWork
}
import uk.ac.wellcome.monitoring.test.fixtures.CloudWatch
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

class MatcherFeatureTest
    extends FunSpec
    with Matchers
    with SQS
    with SNS
    with S3
    with ExtendedPatience
    with Eventually
    with CloudWatch {

  it("receives a message with UnidentifiedWork") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          val work = UnidentifiedWork(
            sourceIdentifier = SourceIdentifier(
              IdentifierSchemes.sierraSystemNumber,
              "Work",
              "id"),
            title = Some("Work"),
            version = 1
          )
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

          withMatcherServer(queue, storageBucket, topic) { _ =>
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

  def withMatcherServer[R](
    queue: Queue,
    bucket: Bucket,
    topic: Topic
  )(testWith: TestWith[EmbeddedHttpServer, R]) = {

    val server: EmbeddedHttpServer =
      new EmbeddedHttpServer(
        new Server(),
        flags = Map(
          "aws.region" -> "localhost"
        ) ++ cloudWatchLocalFlags ++ s3LocalFlags(bucket) ++ sqsLocalFlags(
          queue) ++ snsLocalFlags(topic)
      )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }

}
