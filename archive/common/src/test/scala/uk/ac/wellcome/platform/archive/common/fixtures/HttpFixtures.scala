package uk.ac.wellcome.platform.archive.common.fixtures

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.stream.Materializer
import io.circe.Decoder
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.json.JsonUtil.fromJson
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait HttpFixtures extends Akka with ScalaFutures {
  def whenRequestReady[R](r: HttpRequest)(testWith: TestWith[HttpResponse, R]): R =
    withActorSystem { implicit actorSystem =>
      val request = Http().singleRequest(r)
      whenReady(request) { (response: HttpResponse) =>
        testWith(response)
      }
    }

  def whenGetRequestReady[R](path: String)(testWith: TestWith[HttpResponse, R]): R =
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
}
