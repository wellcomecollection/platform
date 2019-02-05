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

  it("extracts a BagInfo object from a bagInfo file with required fields") {
    val externalIdentifier = randomExternalIdentifier
    val sourceOrganisation = randomSourceOrganisation
    val payloadOxum = randomPayloadOxum
    val baggingDate = randomLocalDate
    val bagInfoString = bagInfoFileContents(
      externalIdentifier,
      sourceOrganisation,
      payloadOxum,
      baggingDate,
      None,
      None,
      None)

    BagInfoParser.parseBagInfo(t, IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Right(
      bagit.BagInfo(
        externalIdentifier,
        sourceOrganisation,
        payloadOxum,
        baggingDate))
  }

  it(
    "extracts a BagInfo object from a bagInfo file with required and optional fields") {
    val externalIdentifier = randomExternalIdentifier
    val sourceOrganisation = randomSourceOrganisation
    val payloadOxum = randomPayloadOxum
    val baggingDate = randomLocalDate
    val externalDescription = Some(randomExternalDescription)
    val internalSenderIdentifier = Some(randomInternalSenderIdentifier)
    val internalSenderDescription = Some(randomInternalSenderDescription)

    val bagInfoString = bagInfoFileContents(
      externalIdentifier,
      sourceOrganisation,
      payloadOxum,
      baggingDate,
      externalDescription,
      internalSenderIdentifier,
      internalSenderDescription
    )

    BagInfoParser.parseBagInfo(t, IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Right(
      bagit.BagInfo(
        externalIdentifier,
        sourceOrganisation,
        payloadOxum,
        baggingDate,
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
    "returns a left of invalid bag info error if there is no source organization in bag-info.txt") {
    val bagInfoString =
      s"""|External-Identifier: $randomExternalIdentifier
          |Payload-Oxum: ${randomPayloadOxum.payloadBytes}.${randomPayloadOxum.numberOfPayloadFiles}
          |Bagging-Date: $randomLocalDate""".stripMargin

    BagInfoParser.parseBagInfo(t, IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(
      InvalidBagInfo(t, List("Source-Organization")))
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
        List(
          "External-Identifier",
          "Source-Organization",
          "Payload-Oxum",
          "Bagging-Date")))
  }

}
