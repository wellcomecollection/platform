package uk.ac.wellcome.platform.archive.call_backerei.fixtures

import com.github.tomakehurst.wiremock.client.WireMock
import uk.ac.wellcome.test.fixtures.TestWith

trait LocalWireMockFixture {
  def withLocalWireMockClient[R](host: String = "localhost", port: Int = 8080)(
    testWith: TestWith[WireMock, R]): R = {
    val wireMock = new WireMock(host, port)
    wireMock.resetRequests()
    testWith(wireMock)
  }
}
