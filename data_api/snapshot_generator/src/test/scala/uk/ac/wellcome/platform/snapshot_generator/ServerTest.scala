package uk.ac.wellcome.platform.snapshot_generator

import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.FunSpec
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.monitoring.fixtures.CloudWatch
import uk.ac.wellcome.test.fixtures.TestWith

class ServerTest
    extends FunSpec
    with SNS
    with CloudWatch
    with fixtures.Server
    with SQS
    with ScalaFutures
    with ElasticsearchFixtures {
  val itemType = documentType

  it("shows the healthcheck message") {
    withFixtures { server =>
      server.httpGet(
        path = "/management/healthcheck",
        andExpect = Ok,
        withJsonBody = """{"message": "ok"}""")
    }
  }

  private def withFixtures[R](testWith: TestWith[EmbeddedHttpServer, R]) =
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalElasticsearchIndex(itemType = itemType) { indexNameV1 =>
          withLocalElasticsearchIndex(itemType = itemType) { indexNameV2 =>
            val flags = snsLocalFlags(topic) ++ sqsLocalFlags(queue) ++ displayEsLocalFlags(
              indexNameV1,
              indexNameV2)
            withServer(flags) { server =>
              testWith(server)
            }
          }
        }
      }
    }
}
