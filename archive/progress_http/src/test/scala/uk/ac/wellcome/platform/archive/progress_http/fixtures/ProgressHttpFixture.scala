package uk.ac.wellcome.platform.archive.progress_http.fixtures

import java.util.UUID

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.stream.Materializer
import com.google.inject.Guice
import io.circe.Decoder
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models
import uk.ac.wellcome.platform.archive.common.progress.models.{ProgressEvent, ProgressUpdate, Progress => ProgressModel}
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorModule
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor
import uk.ac.wellcome.platform.archive.progress_http.modules._
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.{LocalDynamoDb, S3}
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait ProgressHttpFixture
  extends S3
    with RandomThings
    with LocalDynamoDb
    with ScalaFutures
    with ProgressMonitorFixture
    with Messaging {

  import ProgressModel._

  def withProgress[R](monitor: ProgressMonitor)(
    testWith: TestWith[models.Progress, R]) = {
    val id = randomUUID

    val createdProgress =
      ProgressModel(id, uploadUri, Some(callbackUri))

    val storedProgress = monitor.create(createdProgress)

    testWith(storedProgress)
  }

  def withProgressUpdate[R](id: UUID, status: Status = None)(
    testWith: TestWith[ProgressUpdate, R]) = {

    val events = List(
      ProgressEvent(
        description = randomAlphanumeric()
      ))

    val progress = ProgressUpdate(
      id = id,
      events = events,
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
        ProgressMonitorModule
      )
    }
    testWith(progress)
  }

  def withConfiguredApp[R](
                            testWith: TestWith[(Table, String, AkkaHttpApp), R]) = {

    val host = "localhost"
    val port = randomPort
    val baseUrl = s"http://$host:$port"

    val serverConfig = HttpServerConfig(host, port, baseUrl)

    withSpecifiedLocalDynamoDbTable(createProgressMonitorTable) { table =>
      withApp(table, serverConfig) { progressHttp =>
        testWith((table, baseUrl, progressHttp))
      }
    }
  }

  def getT[T](entity: HttpEntity)(implicit decoder: Decoder[T],
                                  materializer: Materializer) = {
    val timeout = 300.millis

    val stringBody = entity
      .toStrict(timeout)
      .map(_.data)
      .map(_.utf8String)
      .value
      .get
      .get
    fromJson[T](stringBody).get
  }

  def whenRequestReady[R](r: HttpRequest)(testWith: TestWith[HttpResponse, R]) =
    withActorSystem { actorSystem =>
      implicit val system = actorSystem

      val request = Http().singleRequest(r)
      whenReady(request) { result =>
        testWith(result)
      }
    }
}
