package uk.ac.wellcome.platform.reindexer.modules

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import com.twitter.finatra.http.EmbeddedHttpServer
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{CalmTransformable, Reindex}
import uk.ac.wellcome.platform.reindexer.Server
import uk.ac.wellcome.platform.reindexer.models.ReindexAttempt
import uk.ac.wellcome.platform.reindexer.services.{
  CalmReindexTargetService,
  ReindexService,
  ReindexTargetService
}
import uk.ac.wellcome.test.utils.{
  AmazonCloudWatchFlag,
  DynamoDBLocal,
  ExtendedPatience
}

import scala.concurrent.Future

class ReindexModuleTest
    extends FunSpec
    with MockitoSugar
    with DynamoDBLocal
    with Eventually
    with ExtendedPatience
    with AmazonCloudWatchFlag
    with Matchers
    with BeforeAndAfterEach {

  val currentVersion = 1
  val requestedVersion = 2

  val dynamoConfigs = Map(
    "reindex" -> DynamoConfig("applicationName",
                              "streamArn",
                              reindexTableName),
    "calm" -> DynamoConfig("applicationName", "streamArn", calmDataTableName)
  )

  var maybeServer: Option[EmbeddedHttpServer] = None

  override def afterEach(): Unit = maybeServer.foreach(_.close())

  it("should retry if the target service returns a failed future") {
    val reindexTargetService = mock[CalmReindexTargetService]
    val server: EmbeddedHttpServer =
      defineServer.bind[ReindexTargetService[CalmTransformable]](
        reindexTargetService)
    maybeServer = Some(server)

    val reindex = Reindex(calmDataTableName, requestedVersion, currentVersion)
    Scanamo.put(dynamoDbClient)(reindexTableName)(reindex)

    when(reindexTargetService.runReindex(ReindexAttempt(reindex, false, 0)))
      .thenReturn(Future.failed(new Exception("boom!")))
      .thenReturn(Future.failed(new Exception("boom!")))
      .thenReturn(Future.failed(new Exception("boom!")))
      .thenReturn(Future.successful(ReindexAttempt(reindex, true, 1)))

    server.start()

    eventually {
      Scanamo.get[Reindex](dynamoDbClient)(reindexTableName)(
        'TableName -> "CalmData") shouldBe Some(
        Right(reindex.copy(CurrentVersion = requestedVersion)))
    }
  }

  it("should call reindexService.run only once if successful") {
    val reindexService = mock[ReindexService[CalmTransformable]]
    val server: EmbeddedHttpServer =
      defineServer.bind[ReindexService[CalmTransformable]](reindexService)
    maybeServer = Some(server)

    when(reindexService.run).thenReturn(Future.successful(()))

    server.start()

    eventually {
      verify(reindexService, times(1)).run
    }
  }

  private def defineServer = {
    new EmbeddedHttpServer(
      new Server(),
      flags = Map(
        "aws.dynamo.reindexTracker.tableName" -> "ReindexTracker",
        "aws.dynamo.calmData.tableName" -> "CalmData",
        "reindex.target.tableName" -> "CalmData"
      ) ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags
    )
  }
}
