package uk.ac.wellcome.platform.archive.common.fixtures

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, Materializer}
import io.circe.Decoder
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.json.JsonUtil.fromJson
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait HttpFixtures extends Akka with ScalaFutures {
  def whenRequestReady[R](r: HttpRequest)(
    testWith: TestWith[HttpResponse, R]): R =
    withActorSystem { implicit actorSystem =>
      val request = Http().singleRequest(r)
      whenReady(request) { (response: HttpResponse) =>
        testWith(response)
      }
    }

  def whenGetRequestReady[R](path: String)(
    testWith: TestWith[HttpResponse, R]): R =
    whenRequestReady(HttpRequest(GET, path)) { response =>
      testWith(response)
    }

  def getT[T](entity: HttpEntity)(implicit decoder: Decoder[T],
                                  materializer: Materializer): T = {
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

  def withStringEntity[R](httpEntity: HttpEntity)(
    testWith: TestWith[String, R])(implicit materializer: ActorMaterializer) = {
    val value =
      httpEntity.dataBytes.runWith(Sink.fold("") {
        case (acc, byteString) =>
          acc + byteString.utf8String
      })
    whenReady(value) { string =>
      testWith(string)
    }
  }

  def createHTTPServerConfig: HTTPServerConfig =
    HTTPServerConfig(
      host = "localhost",
      port = 1234,
      externalBaseURL = "http://localhost:1234"
    )
}
