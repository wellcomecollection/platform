package uk.ac.wellcome.platform.sierra_dynamo.api

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.sierra_dynamo.utils.SierraWireMock

class SierraRetrieverTest extends FunSpec with Matchers with SierraWireMock{


  it("should retrieve records from sierra") {
    val sierraRetriever = new SierraRetriever(sierraWireMockUrl, oauthKey, oauthSecret)

    sierraRetriever.getObjects("items") should have size 50
  }
}
