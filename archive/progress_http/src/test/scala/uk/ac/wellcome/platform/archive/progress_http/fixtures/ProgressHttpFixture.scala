package uk.ac.wellcome.platform.archive.progress_http.fixtures

import java.util.UUID

import akka.http.scaladsl.model.HttpEntity
import akka.stream.Materializer
import com.google.inject.Guice
import io.circe.Decoder
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.platform.archive.common.fixtures.AkkaS3
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models
import uk.ac.wellcome.platform.archive.common.progress.models.{ProgressEvent, ProgressUpdate, Progress => ProgressModel}
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorModule
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor
import uk.ac.wellcome.platform.archive.progress_http.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.progress_http.modules._
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.duration._
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.Random

trait ProgressHttpFixture
  extends AkkaS3
    with LocalDynamoDb
    with ProgressMonitorFixture
    with Messaging {

  val uploadUrl = "uploadUrl"
  val callbackUrl = "http://localhost/archive/complete"

  import ProgressModel._

  private def randomAlphanumeric(length: Int = 8) = {
    Random.alphanumeric take length mkString
  }

  private def randomPort = {
    val startPort = 10000
    val portRange = 10000

    startPort + Random.nextInt(portRange)
  }

  private def generateUUID = UUID.randomUUID().toString

  def withProgress[R](monitor: ProgressMonitor)(
    testWith: TestWith[models.Progress, R]) = {
    val id = generateUUID

    val createdProgress =
      ProgressModel(id, uploadUrl, Some(callbackUrl))

    val storedProgress = monitor.create(createdProgress)

    testWith(storedProgress)
  }

  def withProgressUpdate[R](id: String, status: Status = None)(
    testWith: TestWith[ProgressUpdate, R]) = {

    val event = ProgressEvent(
      description = randomAlphanumeric()
    )

    val progress = ProgressUpdate(
      id = id,
      event = event,
      status = status
    )

    testWith(progress)
  }

  def withApp[R](table: Table, serverConfig: HttpServerConfig)(
    testWith: TestWith[AkkaHttpApp, R]) = {

    val progress = new AkkaHttpApp {
      val injector = Guice.createInjector(
        new TestAppConfigModule(table, serverConfig),
        ConfigModule,
        AkkaModule,
        CloudWatchClientModule,
        HttpStreamModule,
        RequestHandlerModule,
        ProgressMonitorModule
      )
    }
    testWith(progress)
  }

  def withConfiguredApp[R](testWith: TestWith[(Table, String, AkkaHttpApp), R]) = {

    val host = "localhost"
    val port = randomPort

    val serverConfig = HttpServerConfig(host, port)

    val baseUrl = s"http://$host:$port"

    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) {
      table =>
        withApp(table, serverConfig) { progressHttp =>
          testWith((table, baseUrl, progressHttp))
        }
    }
  }

  def getT[T](entity: HttpEntity)(implicit decoder: Decoder[T], materializer: Materializer) = {
    val timeout = 300.millis

    val stringBody = entity.toStrict(timeout)
      .map(_.data)
      .map(_.utf8String)
      .value
      .get
      .get

    fromJson[T](stringBody).get
  }
}
