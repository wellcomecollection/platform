package uk.ac.wellcome.platform.snapshot_generator

import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.FunSpec
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.SNS.Topic
import uk.ac.wellcome.test.fixtures.SQS.Queue
import uk.ac.wellcome.test.fixtures.{S3, SNS, SQS, TestWith}

class ServerTest
    extends FunSpec
    with S3
    with SNS
    with SQS
    with ScalaFutures
    with ElasticsearchFixtures
    with fixtures.Server {
  val itemType = "work"
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

  it("shows the healthcheck message") {
    withFixtures {
      case (server, _, _, _, _, _) =>
        server.httpGet(
          path = "/management/healthcheck",
          andExpect = Ok,
          withJsonBody = """{"message": "ok"}""")
    }
  }
}
