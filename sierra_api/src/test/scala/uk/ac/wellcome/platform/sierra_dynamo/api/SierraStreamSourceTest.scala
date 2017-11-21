package uk.ac.wellcome.platform.sierra_dynamo.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlMatching}
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.circe.Json
import io.circe.optics.JsonPath
import io.circe.optics.JsonPath.root
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.sierra_dynamo.utils.SierraWireMock

class SierraStreamSourceTest
    extends FunSpec
    with SierraWireMock
    with Matchers
    with ScalaFutures
    with ExtendedPatience {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  it("should read from sierra") {
    val eventualJson = SierraSource(sierraWireMockUrl, oauthKey, oauthSecret)(
      "items").take(1).runWith(Sink.head[Json])
    whenReady(eventualJson) { json =>
      root.id.string.getOption(json) shouldBe Some("1000001")
    }
  }

  it("should paginate through results") {
    val sierraSource = SierraSource(sierraWireMockUrl, oauthKey, oauthSecret)(
      "items",
      Map("updatedDate" -> "[2013-12-10T17:16:35Z,2013-12-13T21:34:35Z]"))

    val eventualJsonList = sierraSource.runWith(Sink.seq[Json])

    whenReady(eventualJsonList) { jsonList =>
      jsonList should have size 157
    }
  }

  it("should refresh the access token if receives a unauthorized response") {
    stubFor(get(urlMatching("/bibs")).inScenario("refresh token")
      .whenScenarioStateIs(Scenario.STARTED).willReturn(aResponse().withStatus(401))
      .atPriority(1).willSetStateTo("token expired"))

    stubFor(get(urlMatching("/token")).inScenario("refresh token")
      .whenScenarioStateIs("token expired").willSetStateTo("token refreshed"))

    stubFor(get(urlMatching("/bibs")).inScenario("refresh token")
      .whenScenarioStateIs("token refreshed"))

    val eventualJson = SierraSource(sierraWireMockUrl, oauthKey, oauthSecret)(
      "bibs").take(1).runWith(Sink.head[Json])

    whenReady(eventualJson) { json =>
      root.id.string.getOption(json) shouldBe Some("1000001")
    }
  }

  it("should return a sensible error message if it fails to authorize with the sierra api") {
    stubFor(get(urlMatching("/bibs")).willReturn(aResponse().withStatus(401)))

    val eventualJson = SierraSource(sierraWireMockUrl, oauthKey, oauthSecret)("bibs").take(1).runWith(Sink.head[Json])

    whenReady(eventualJson.failed) { ex =>
      ex shouldBe a [RuntimeException]
      ex.getMessage should include ("Unauthorized")
    }
  }

}

trait ExtendedPatience extends PatienceConfiguration {
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(40, Seconds)),
    interval = scaled(Span(150, Millis))
  )
}
