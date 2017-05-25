package uk.ac.wellcome.platform.reindexer

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.{FunSpec, Matchers}

class ReindexerFeatureTest
  extends FunSpec
    with Matchers {

  val server: EmbeddedHttpServer =
    new EmbeddedHttpServer(
      new Server(),
      flags = Map(
        "aws.dynamo.reindexTracker.streams.appName" -> "reindex",
        "aws.dynamo.reindexTracker.streams.arn" -> "reindex",
        "aws.dynamo.reindexTracker.tableName" -> "ReindexTracker",
        "aws.dynamo.miroData.streams.appName" -> "mirodata",
        "aws.dynamo.miroData.streams.arn" -> "mirodata",
        "aws.dynamo.miroData.tableName" -> "MiroData",
        "reindex.target.tableName" -> "MiroData"
      )
    )

  it("should not fail") {
    true shouldBe false
  }
}
