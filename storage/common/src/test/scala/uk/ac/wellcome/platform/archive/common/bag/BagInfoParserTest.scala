package uk.ac.wellcome.platform.archive.common.bag

import org.apache.commons.io.IOUtils
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.common.fixtures.{BagIt, RandomThings}
import uk.ac.wellcome.platform.archive.common.models.bagit
import uk.ac.wellcome.platform.archive.common.models.error.InvalidBagInfo

class BagInfoParserTest
    extends FunSpec
    with BagIt
    with Matchers
    with RandomThings {
  case class Thing(id: String)
  val t = Thing("a")

  it("extracts a BagInfo object from a bagInfo file with only required fields") {
    val externalIdentifier = randomExternalIdentifier
    val payloadOxum = randomPayloadOxum
    val baggingDate = randomLocalDate
    val bagInfoString =
      bagInfoFileContents(externalIdentifier, payloadOxum, baggingDate)

    BagInfoParser.parseBagInfo(t, IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Right(
      bagit.BagInfo(externalIdentifier, payloadOxum, baggingDate))
  }

  it(
    "extracts a BagInfo object from a bagInfo file with all required and optional fields") {
    val externalIdentifier = randomExternalIdentifier
    val payloadOxum = randomPayloadOxum
    val baggingDate = randomLocalDate
    val sourceOrganisation = Some(randomSourceOrganisation)
    val externalDescription = Some(randomExternalDescription)
    val internalSenderIdentifier = Some(randomInternalSenderIdentifier)
    val internalSenderDescription = Some(randomInternalSenderDescription)

    val bagInfoString = bagInfoFileContents(
      externalIdentifier,
      payloadOxum,
      baggingDate,
      sourceOrganisation,
      externalDescription,
      internalSenderIdentifier,
      internalSenderDescription)

    BagInfoParser.parseBagInfo(t, IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Right(
      bagit.BagInfo(
        externalIdentifier,
        payloadOxum,
        baggingDate,
        sourceOrganisation,
        externalDescription,
        internalSenderIdentifier,
        internalSenderDescription))
  }

  it(
    "returns a left of invalid bag info error if there is no external identifier in bag-info.txt") {
    val bagInfoString =
      s"""|Source-Organization: $randomSourceOrganisation
          |Payload-Oxum: ${randomPayloadOxum.payloadBytes}.${randomPayloadOxum.numberOfPayloadFiles}
          |Bagging-Date: $randomLocalDate""".stripMargin

    BagInfoParser.parseBagInfo(t, IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(
      InvalidBagInfo(t, List("External-Identifier")))
  }

  it(
    "returns a left of invalid bag info error if there is no payload-oxum in bag-info.txt") {
    val bagInfoString =
      s"""|External-Identifier: $randomExternalIdentifier
          |Source-Organization: $randomSourceOrganisation
          |Bagging-Date: $randomLocalDate""".stripMargin

    BagInfoParser.parseBagInfo(t, IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(
      InvalidBagInfo(t, List("Payload-Oxum")))
  }

  it(
    "returns a left of invalid bag info error if the payload-oxum is invalid in bag-info.txt") {
    val bagInfoString =
      s"""|External-Identifier: $randomExternalIdentifier
          |Source-Organization: $randomSourceOrganisation
          |Payload-Oxum: sgadfjag
          |Bagging-Date: $randomLocalDate""".stripMargin

    BagInfoParser.parseBagInfo(t, IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(
      InvalidBagInfo(t, List("Payload-Oxum")))
  }

  it(
    "returns a left of invalid bag info error if there is no bagging date in bag-info.txt") {
    val bagInfoString =
      s"""|External-Identifier: $randomExternalIdentifier
          |Source-Organization: $randomSourceOrganisation
          |Payload-Oxum: ${randomPayloadOxum.payloadBytes}.${randomPayloadOxum.numberOfPayloadFiles}""".stripMargin

    BagInfoParser.parseBagInfo(t, IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(
      InvalidBagInfo(t, List("Bagging-Date")))
  }

  it(
    "returns a left of invalid bag info error if the bagging date is invalid in bag-info.txt") {
    val bagInfoString =
      s"""|External-Identifier: $randomExternalIdentifier
          |Source-Organization: $randomSourceOrganisation
          |Payload-Oxum: ${randomPayloadOxum.payloadBytes}.${randomPayloadOxum.numberOfPayloadFiles}
          |Bagging-Date: sdfkjghl""".stripMargin

    BagInfoParser.parseBagInfo(t, IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(
      InvalidBagInfo(t, List("Bagging-Date")))
  }

  it("returns a left of invalid bag info error if bag-info.txt is empty") {
    val bagInfoString = ""

    BagInfoParser.parseBagInfo(t, IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(
      InvalidBagInfo(
        t,
        List("External-Identifier", "Payload-Oxum", "Bagging-Date")))
  }

}
