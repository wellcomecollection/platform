package uk.ac.wellcome.platform.archive.common.fixtures

import java.net.URL

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.{GET, POST}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, Materializer, QueueOfferResult}
import io.circe.Decoder
import org.mockito.Mockito.{atLeastOnce, verify}
import org.scalatest.{Assertion, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.json.JsonUtil.fromJson
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.archive.common.http.HttpMetricResults
import uk.ac.wellcome.platform.archive.common.http.models.ErrorResponse
import uk.ac.wellcome.test.fixtures.Akka

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

trait HttpFixtures extends Akka with ScalaFutures { this: Matchers =>
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import uk.ac.wellcome.json.JsonUtil._

  private def whenRequestReady[R](r: HttpRequest)(
    testWith: TestWith[HttpResponse, R]): R =
    withActorSystem { implicit actorSystem =>
      val request = Http().singleRequest(r)
      whenReady(request) { response: HttpResponse =>
        testWith(response)
      }
    }

  def whenGetRequestReady[R](path: String)(
    testWith: TestWith[HttpResponse, R]): R =
    whenRequestReady(HttpRequest(GET, path)) { response =>
      testWith(response)
    }

  def whenPostRequestReady[R](url: String, entity: RequestEntity)(
    testWith: TestWith[HttpResponse, R]): R = {
    val request = HttpRequest(
      method = POST,
      uri = url,
      headers = Nil,
      entity = entity
    )

    whenRequestReady(request) { response =>
      testWith(response)
    }
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

  val httpServerConfig: HTTPServerConfig = createHTTPServerConfig

  val metricsName: String

  def assertMetricSent(
    metricsSender: MetricsSender,
    result: HttpMetricResults.Value): Future[QueueOfferResult] =
    verify(metricsSender, atLeastOnce())
      .incrementCount(metricName = s"${metricsName}_HttpResponse_$result")

  val contextURL: URL

  def assertIsErrorResponse(response: HttpResponse,
                            description: String,
                            statusCode: StatusCode = StatusCodes.BadRequest,
                            label: String = "Bad Request")(
    implicit materializer: ActorMaterializer): Assertion = {
    response.status shouldBe statusCode
    response.entity.contentType shouldBe ContentTypes.`application/json`

    val progressFuture = Unmarshal(response.entity).to[ErrorResponse]

    whenReady(progressFuture) { actualError =>
      actualError shouldBe ErrorResponse(
        context = contextURL.toString,
        httpStatus = statusCode.intValue(),
        description = Some(description),
        label = label
      )
    }
  }
}
