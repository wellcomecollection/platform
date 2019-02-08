package uk.ac.wellcome.platform.storage.bags.api.models

import java.time.format.DateTimeFormatter

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings

class DisplayBagInfoTest extends FunSpec with RandomThings with Matchers {

  it("transforms a BagInfo with all fields into a DisplayBagInfo") {
    val bagInfo = randomBagInfo
    DisplayBagInfo(bagInfo) shouldBe DisplayBagInfo(
      bagInfo.externalIdentifier.underlying,
      s"${bagInfo.payloadOxum.payloadBytes}.${bagInfo.payloadOxum.numberOfPayloadFiles}",
      bagInfo.sourceOrganisation.underlying,
      bagInfo.baggingDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
      Some(bagInfo.externalDescription.get.underlying),
      Some(bagInfo.internalSenderIdentifier.get.underlying),
      Some(bagInfo.internalSenderDescription.get.underlying),
    )
  }

}
