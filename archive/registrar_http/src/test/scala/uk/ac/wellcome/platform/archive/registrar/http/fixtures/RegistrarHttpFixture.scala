package uk.ac.wellcome.platform.archive.registrar.http.fixtures

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.stream.Materializer
import com.google.inject.Guice
import io.circe.Decoder
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.platform.archive.registrar.common.modules.VHSModule
import uk.ac.wellcome.platform.archive.registrar.http.modules.{AkkaHttpApp, ConfigModule, TestAppConfigModule}
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait RegistrarHttpFixture
  extends LocalVersionedHybridStore
    with RandomThings
    with ScalaFutures with Akka {

  def withApp[R](table: Table, bucket: Bucket, s3Prefix: String, serverConfig: HttpServerConfig)(
    testWith: TestWith[AkkaHttpApp, R]) = {

    val progress = new AkkaHttpApp {
      val injector = Guice.createInjector(
        new TestAppConfigModule(serverConfig, table.name, bucket.name, s3Prefix),
        ConfigModule,
        AkkaModule,
        VHSModule,
        DynamoClientModule
      )
    }
    testWith(progress)
  }

  def withConfiguredApp[R](
    testWith: TestWith[(VersionedHybridStore[StorageManifest, EmptyMetadata, ObjectStore[StorageManifest]], String, AkkaHttpApp), R]) = {

    val host = "localhost"
    val port = randomPort
    val baseUrl = s"http://$host:$port"

    val serverConfig = HttpServerConfig(host, port, baseUrl)

    withLocalS3Bucket { bucket =>
    withLocalDynamoDbTable { table =>
      val s3Prefix = "archive"
      withTypeVHS[StorageManifest, EmptyMetadata, R](bucket, table, s3Prefix) { vhs =>
        withApp(table, bucket, s3Prefix, serverConfig) { progressHttp =>
          testWith((vhs, baseUrl, progressHttp))
        }
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
    withActorSystem { actorSystem =>
      implicit val system = actorSystem

      val request = Http().singleRequest(r)
      whenReady(request) { result =>
        testWith(result)
      }
    }
}
