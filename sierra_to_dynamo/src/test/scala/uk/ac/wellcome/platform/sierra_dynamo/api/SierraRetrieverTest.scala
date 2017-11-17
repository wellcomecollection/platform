package uk.ac.wellcome.platform.sierra_dynamo.api

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.sierra_dynamo.utils.SierraWireMock

class SierraRetrieverTest extends FunSpec with Matchers with SierraWireMock{
  val sierraRetriever = new SierraRetriever(sierraWireMockUrl, oauthKey, oauthSecret)

  it("should retrieve records from sierra") {
    sierraRetriever.getObjects("items") shouldNot be (empty)
  }

  it("should paginate through results") {
    sierraRetriever.getObjects("items",
      Map("updatedDate" -> "[2013-12-10T17:16:35Z,2013-12-13T21:34:35Z]")) should have size 157
  }

  it("should transparently refresh the access token if it has expired") {
    stubFor(get(urlMatching("/bibs")).inScenario("refresh token")
      .whenScenarioStateIs(Scenario.STARTED).willReturn(aResponse().withStatus(401))
      .atPriority(1).willSetStateTo("token expired"))

    stubFor(get(urlMatching("/token")).inScenario("refresh token")
      .whenScenarioStateIs("token expired").willSetStateTo("token refreshed"))

    stubFor(get(urlMatching("/bibs")).inScenario("refresh token")
      .whenScenarioStateIs("token refreshed"))

    sierraRetriever.getObjects("bibs") shouldNot be (empty)
  }
}
