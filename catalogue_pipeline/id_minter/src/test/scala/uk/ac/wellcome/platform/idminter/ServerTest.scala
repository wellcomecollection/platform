package uk.ac.wellcome.platform.idminter
import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import org.scalatest.FunSpec
import uk.ac.wellcome.test.fixtures.{SNS, SQS}

class ServerTest
    extends FunSpec
    with fixtures.Server
    with SQS
    with SNS
    with fixtures.IdentifiersDatabase {

  it("shows the healthcheck message") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topic =>
        withIdentifiersDatabase { dbConfig =>
          val flags = sqsLocalFlags(queueUrl) ++ snsLocalFlags(topic) ++ dbConfig.flags

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
