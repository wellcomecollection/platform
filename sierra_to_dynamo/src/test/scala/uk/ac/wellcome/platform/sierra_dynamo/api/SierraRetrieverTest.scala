package uk.ac.wellcome.platform.sierra_dynamo.api

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.sierra_dynamo.utils.SierraWireMock

class SierraRetrieverTest extends FunSpec with Matchers with SierraWireMock{
  val sierraRetriever = new SierraRetriever(sierraWireMockUrl, oauthKey, oauthSecret)

  it("should retrieve records from sierra") {
    sierraRetriever.getObjects("items") should have size 50
  }

  it("should paginate through results") {
    sierraRetriever.getObjects("items", Map("updatedDate" -> "[2013-12-10T17:16:35Z,2013-12-13T21:34:35Z]")) should have size 197
  }
}
