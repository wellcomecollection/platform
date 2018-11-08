package uk.ac.wellcome.platform.archive.registrar.http.fixtures

import java.net.URL

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.stream.Materializer
import io.circe.Decoder
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.platform.archive.registrar.http.RegistrarHTTP
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
    with ScalaFutures
    with Akka {

  def withApp[R](table: Table,
                 bucket: Bucket,
                 s3Prefix: String,
                 httpServerConfig: HTTPServerConfig,
                 contextURL: URL)(testWith: TestWith[RegistrarHTTP, R]): R =
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
        withTypeVHS[StorageManifest, EmptyMetadata, R](bucket, table) { vhs =>
          val registrarHTTP = new RegistrarHTTP(
            vhs = vhs,
            httpServerConfig = httpServerConfig,
            contextURL = contextURL
          )(
            actorSystem = actorSystem,
            materializer = materializer,
            executionContext = actorSystem.dispatcher
          )

          testWith(registrarHTTP)
        }
      }
    }

  def withConfiguredApp[R](
    testWith: TestWith[(VersionedHybridStore[StorageManifest,
                                             EmptyMetadata,
                                             ObjectStore[StorageManifest]],
                        String,
                        RegistrarHTTP),
                       R]): R = {
    val host = "localhost"
    val port = randomPort
    val externalBaseURL = s"http://$host:$port"
    val contextURL = new URL(
      "http://api.wellcomecollection.org/storage/v1/context.json")

    val httpServerConfig = HTTPServerConfig(
      host = host,
      port = port,
      externalBaseURL = externalBaseURL
    )

    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        val s3Prefix = "archive"
        withTypeVHS[StorageManifest, EmptyMetadata, R](bucket, table, s3Prefix) {
          vhs =>
            withApp(table, bucket, s3Prefix, httpServerConfig, contextURL) {
              progressHttp =>
                testWith((vhs, externalBaseURL, progressHttp))
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
    withActorSystem { implicit actorSystem =>
      val request = Http().singleRequest(r)
      whenReady(request) { result =>
        testWith(result)
      }
    }
}
