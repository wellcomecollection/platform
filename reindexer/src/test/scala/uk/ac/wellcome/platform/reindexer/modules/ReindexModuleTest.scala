package uk.ac.wellcome.platform.reindexer.modules

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import com.twitter.finatra.http.EmbeddedHttpServer
import org.mockito.Mockito.when
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{CalmTransformable, Reindex}
import uk.ac.wellcome.platform.reindexer.Server
import uk.ac.wellcome.platform.reindexer.models.ReindexAttempt
import uk.ac.wellcome.platform.reindexer.services.{CalmReindexTargetService, ReindexTargetService}
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, DynamoDBLocal, ExtendedPatience}

import scala.concurrent.Future

class ReindexModuleTest
    extends FunSpec
    with MockitoSugar
    with DynamoDBLocal
    with Eventually
    with ExtendedPatience
    with AmazonCloudWatchFlag
    with Matchers {

  val reindexTargetService = mock[CalmReindexTargetService]
  val server: EmbeddedHttpServer =
    new EmbeddedHttpServer(
      new Server(),
      flags = Map(
        "aws.dynamo.reindexTracker.tableName" -> "ReindexTracker",
        "aws.dynamo.calmData.tableName" -> "CalmData",
        "reindex.target.tableName" -> "CalmData"
      ) ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags
    ).bind[ReindexTargetService[CalmTransformable]](reindexTargetService)

  val currentVersion = 1
  val requestedVersion = 2

  val dynamoConfigs = Map(
    "reindex" -> DynamoConfig("applicationName",
                              "streamArn",
                              reindexTableName),
    "calm" -> DynamoConfig("applicationName", "streamArn", calmDataTableName)
  )

  it("should retry if the target service returns a failed future") {
    val reindex = Reindex(calmDataTableName, requestedVersion, currentVersion)
    Scanamo.put(dynamoDbClient)(reindexTableName)(reindex)

    when(reindexTargetService.runReindex(ReindexAttempt(reindex, Nil, 0)))
      .thenReturn(Future.failed(new Exception("boom!")))
      .thenReturn(Future.failed(new Exception("boom!")))
      .thenReturn(Future.failed(new Exception("boom!")))
      .thenReturn(Future.successful(ReindexAttempt(reindex, Nil, 1)))

    server.start()

    eventually {
      Scanamo.get[Reindex](dynamoDbClient)(reindexTableName)(
        'TableName -> "CalmData") shouldBe Some(
        Right(reindex.copy(CurrentVersion = requestedVersion)))
    }

    server.close()
  }
}
