package uk.ac.wellcome.platform.reindexer.modules

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.syntax._
import com.twitter.finatra.http.EmbeddedHttpServer
import org.mockito.Matchers.{any, eq => mockitoEquals}
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.Reindex
import uk.ac.wellcome.models.transformable.{CalmTransformable, Reindexable}
import uk.ac.wellcome.platform.reindexer.Server
import uk.ac.wellcome.platform.reindexer.models.ReindexAttempt
import uk.ac.wellcome.platform.reindexer.services.ReindexTargetService
import uk.ac.wellcome.platform.reindexer.locals.DynamoDBLocal
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, ExtendedPatience}

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

  var maybeServer: Option[EmbeddedHttpServer] = None

  override def afterEach(): Unit = maybeServer.foreach(_.close())

  it("should retry if the target service returns a failed future") {
    val reindexTargetService = mock[ReindexTargetService[CalmTransformable]]
    val server: EmbeddedHttpServer =
      defineServer.bind[ReindexTargetService[CalmTransformable]](
        reindexTargetService)
    maybeServer = Some(server)

    val reindex = Reindex(calmDataTableName,
                          reindexShard,
                          requestedVersion,
                          currentVersion)
    Scanamo.put(dynamoDbClient)(reindexTableName)(reindex)

    when(
      reindexTargetService.runReindex(
        mockitoEquals(ReindexAttempt(reindex, false, 0)))(
        any[DynamoFormat[CalmTransformable]]))
      .thenReturn(Future.failed(new Exception("boom!")))
      .thenReturn(Future.failed(new Exception("boom!")))
      .thenReturn(Future.failed(new Exception("boom!")))
      .thenReturn(Future.successful(ReindexAttempt(reindex, true, 1)))

    server.start()

    eventually {
      Scanamo.get[Reindex](dynamoDbClient)(reindexTableName)(
        'TableName -> "CalmData" and 'ReindexShard -> reindexShard) shouldBe Some(
        Right(reindex.copy(CurrentVersion = requestedVersion)))
    }
  }

  it("should call reindexService.run only once if successful") {
    val reindexService = mock[ReindexService[Reindexable[String]]]
    val server: EmbeddedHttpServer =
      defineServer.bind[ReindexService[Reindexable[String]]](reindexService)
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
        "aws.dynamo.tableName" -> "ReindexTracker",
        "reindex.target.tableName" -> "CalmData"
      ) ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags
    )
  }
}
