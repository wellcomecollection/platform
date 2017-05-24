package uk.ac.wellcome.platform.reindexer
import com.google.inject.Stage
import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class ServerTest extends FeatureTest {

  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new Server,
    flags = Map(
      "aws.dynamo.reindexTracker.streams.appName" -> "reindex",
      "aws.dynamo.reindexTracker.streams.arn" -> "reindex",
      "aws.dynamo.reindexTracker.tableName" -> "reindex"
    )
  )

  test("it should show the healthcheck message") {
    server.httpGet(path = "/management/healthcheck",
                   andExpect = Ok,
                   withJsonBody = """{"message": "ok"}""")
  }
}
