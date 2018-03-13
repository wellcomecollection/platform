package uk.ac.wellcome.platform.snapshot_convertor

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.Matchers
import org.scalatest.FunSpec
import uk.ac.wellcome.test.fixtures._
import scala.collection.JavaConversions._

class SnapshotConvertorFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with AmazonCloudWatchFlag
    with SqsFixtures
    with SnsFixtures
    with S3
    with ScalaFutures {

  def withServer[R](queueUrl: String, topicArn: String, bucketName: String)(
    testWith: TestWith[EmbeddedHttpServer, R]) = {
    val server: EmbeddedHttpServer =
      new EmbeddedHttpServer(
        new Server(),
        flags = Map(
          "aws.s3.bucketName" -> bucketName,
          "aws.region" -> "eu-west-1",
          "aws.sns.topic.arn" -> topicArn,
          "aws.sqs.queue.url" -> queueUrl
        ) ++ snsLocalFlags ++ cloudWatchLocalEndpointFlag ++ sqsLocalFlags ++ s3LocalFlags
      )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }

  it("does not fail") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topicArn =>
        withLocalS3Bucket { bucketname =>
          withServer(queueUrl, topicArn, bucketName: String) { server =>
            true shouldBe false

          }
        }
      }
    }
  }
}
