package uk.ac.wellcome.platform.idminter
import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import org.scalatest.FunSpec
import uk.ac.wellcome.test.fixtures.{SnsFixtures, SqsFixtures}

class ServerTest
  extends FunSpec
    with fixtures.Server
    with SqsFixtures
    with SnsFixtures
    with fixtures.IdentifiersDatabase {

  it("shows the healthcheck message") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topicArn =>
        withIdentifiersDatabase { dbConfig =>

          val flags = Map(
            "aws.region" -> "localhost",
            "aws.sqs.queue.url" -> queueUrl,
            "aws.sqs.waitTime" -> "1",
            "aws.sns.topic.arn" -> topicArn
          ) ++ sqsLocalFlags ++ snsLocalFlags ++ dbConfig.flags

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
