package uk.ac.wellcome.platform.archive.progress_http.fixtures

import java.util.UUID

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.stream.Materializer
import com.google.inject.Guice
import io.circe.Decoder
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS}
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{ProgressGenerators, ProgressTrackerFixture}
import uk.ac.wellcome.platform.archive.common.progress.models.{Progress, ProgressEvent, ProgressStatusUpdate, ProgressUpdate}
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressTrackerModule
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
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
    with ProgressTrackerFixture
    with ProgressGenerators
      with SNS
    with Messaging {

//  import Progress._

  def withProgress[R](monitor: ProgressTracker)(
    testWith: TestWith[Progress, R]) = {
    val createdProgress = createProgress

    whenReady(monitor.initialise(createdProgress)) { storedProgress=>

      testWith(storedProgress)
    }
  }

  def withProgressUpdate[R](id: UUID, status: Progress.Status)(
    testWith: TestWith[ProgressUpdate, R]) = {

    val events = List(
      ProgressEvent(
        description = randomAlphanumeric()
      ))

    val progress = ProgressStatusUpdate(
      id = id,
      status = status,
      affectedResources = List(createResource),
      events = events
    )

    testWith(progress)
  }

  def withApp[R](table: Table, topic: Topic,serverConfig: HttpServerConfig)(
    testWith: TestWith[AkkaHttpApp, R]) = {

    val progress = new AkkaHttpApp {
      val injector = Guice.createInjector(
        new TestAppConfigModule(table, topic, serverConfig),
        ConfigModule,
        AkkaModule,
        CloudWatchClientModule,
        ProgressTrackerModule,
        SNSClientModule
      )
    }
    testWith(progress)
  }

  def withConfiguredApp[R](
    testWith: TestWith[(Table, Topic, String, AkkaHttpApp), R]) = {

    val host = "localhost"
    val port = randomPort
    val baseUrl = s"http://$host:$port"

    val serverConfig = HttpServerConfig(host, port, baseUrl)

    withLocalSnsTopic { topic =>
    withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
      withApp(table, topic, serverConfig) { progressHttp =>
        testWith((table, topic, baseUrl, progressHttp))
      }
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
    withActorSystem { implicit actorSystem =>

      val request = Http().singleRequest(r)
      whenReady(request) { result =>
        testWith(result)
      }
    }
}
