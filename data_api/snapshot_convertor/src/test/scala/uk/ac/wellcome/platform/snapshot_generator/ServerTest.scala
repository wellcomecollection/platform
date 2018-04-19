package uk.ac.wellcome.platform.snapshot_generator

import com.twitter.finagle.http.Status.Ok
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.FunSpec
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.test.fixtures.{S3, SNS, SQS}

class ServerTest
    extends FunSpec
    with S3
    with SNS
    with SQS
    with ScalaFutures
    with ElasticsearchFixtures
    with fixtures.Server {
  val itemType = "work"

  it("shows the healthcheck message") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withLocalElasticsearchIndex(itemType = itemType) { indexNameV1 =>
            withLocalElasticsearchIndex(itemType = itemType) { indexNameV2 =>
              val flags = snsLocalFlags(topic) ++ sqsLocalFlags(queue) ++ s3LocalFlags(
                bucket) ++ esLocalFlags(indexNameV1, indexNameV2, itemType)
              withServer(flags) { server =>
                server.httpGet(
                  path = "/management/healthcheck",
                  andExpect = Ok,
                  withJsonBody = """{"message": "ok"}""")
              }
            }
          }
        }
      }
    }
  }
}
