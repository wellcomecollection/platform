package uk.ac.wellcome.platform.snapshot_generator

import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.FunSpec
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.monitoring.test.fixtures.CloudWatch
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

class ServerTest
    extends FunSpec
    with S3
    with SNS
    with CloudWatch
    with SQS
    with ScalaFutures
    with ElasticsearchFixtures {
  val itemType = "work"

  it("shows the healthcheck message") {
    withFixtures {
      case (server, _, _, _, _, _) =>
        server.httpGet(
          path = "/management/healthcheck",
          andExpect = Ok,
          withJsonBody = """{"message": "ok"}""")
    }
  }

  def withFixtures[R](
    testWith: TestWith[
      (EmbeddedHttpServer, Queue, Topic, String, String, Bucket),
      R]) =
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalElasticsearchIndex(itemType = itemType) { indexNameV1 =>
          withLocalElasticsearchIndex(itemType = itemType) { indexNameV2 =>
            withLocalS3Bucket { bucket =>
              val flags = snsLocalFlags(topic) ++ sqsLocalFlags(queue) ++ s3LocalFlags(
                bucket) ++ esLocalFlags(indexNameV1, indexNameV2, itemType)
              withServer(flags) { server =>
                testWith(
                  (server, queue, topic, indexNameV1, indexNameV2, bucket))
              }
            }
          }
        }
      }
    }

  def withServer[R](flags: Map[String, String])(
    testWith: TestWith[EmbeddedHttpServer, R]) = {
    val server: EmbeddedHttpServer =
      new EmbeddedHttpServer(
        new Server(),
        flags = flags ++ cloudWatchLocalFlags
      )

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }
}
