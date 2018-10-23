package uk.ac.wellcome.platform.archive.archivist.bag
import org.apache.commons.io.IOUtils
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.archivist.models.IngestRequestContextGenerators
import uk.ac.wellcome.platform.archive.archivist.models.errors.InvalidBagInfo
import uk.ac.wellcome.platform.archive.common.fixtures.{BagIt, RandomThings}
import uk.ac.wellcome.platform.archive.common.models.BagInfo

class BagInfoParserTest
    extends FunSpec
    with BagIt
    with Matchers
    with IngestRequestContextGenerators
    with RandomThings {
  val request = createIngestBagRequest

  it("extracts a BagInfo object from a valid bagInfo file") {
    val externalIdentifier = randomExternalIdentifier
    val sourceOrganisation = randomSourceOrganisation
    val payloadOxum = randomPayloadOxum
    val baggingDate = randomLocalDate
    val bagInfoString = bagInfoFileContents(
      externalIdentifier,
      sourceOrganisation,
      payloadOxum,
      baggingDate)

    BagInfoParser.parseBagInfo(
      request,
      IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Right(
      BagInfo(externalIdentifier, sourceOrganisation, payloadOxum, baggingDate))
  }

  it(
    "returns a left of invalid bag info error if there is no external identifier in bag-info.txt") {
    val bagInfoString =
      s"""|Source-Organization: $randomSourceOrganisation
          |Payload-Oxum: ${randomPayloadOxum.payloadBytes}.${randomPayloadOxum.numberOfPayloadFiles}
          |Bagging-Date: $randomLocalDate""".stripMargin

    BagInfoParser.parseBagInfo(
      request,
      IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(
      InvalidBagInfo(request, List("External-Identifier")))
  }

  it(
    "returns a left of invalid bag info error if there is no source organization in bag-info.txt") {
    val bagInfoString =
      s"""|External-Identifier: $randomExternalIdentifier
          |Payload-Oxum: ${randomPayloadOxum.payloadBytes}.${randomPayloadOxum.numberOfPayloadFiles}
          |Bagging-Date: $randomLocalDate""".stripMargin

    BagInfoParser.parseBagInfo(
      request,
      IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(
      InvalidBagInfo(request, List("Source-Organization")))
  }

  it(
    "returns a left of invalid bag info error if there is no payload-oxum in bag-info.txt") {
    val bagInfoString =
      s"""|External-Identifier: $randomExternalIdentifier
          |Source-Organization: $randomSourceOrganisation
          |Bagging-Date: $randomLocalDate""".stripMargin

    BagInfoParser.parseBagInfo(
      request,
      IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(
      InvalidBagInfo(request, List("Payload-Oxum")))
  }

  it(
    "returns a left of invalid bag info error if the payload-oxum is invalid in bag-info.txt") {
    val bagInfoString =
      s"""|External-Identifier: $randomExternalIdentifier
          |Source-Organization: $randomSourceOrganisation
          |Payload-Oxum: sgadfjag
          |Bagging-Date: $randomLocalDate""".stripMargin

    BagInfoParser.parseBagInfo(
      request,
      IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(
      InvalidBagInfo(request, List("Payload-Oxum")))
  }

  it(
    "returns a left of invalid bag info error if there is no bagging date in bag-info.txt") {
    val bagInfoString =
      s"""|External-Identifier: $randomExternalIdentifier
          |Source-Organization: $randomSourceOrganisation
          |Payload-Oxum: ${randomPayloadOxum.payloadBytes}.${randomPayloadOxum.numberOfPayloadFiles}""".stripMargin

    BagInfoParser.parseBagInfo(
      request,
      IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(
      InvalidBagInfo(request, List("Bagging-Date")))
  }

  it(
    "returns a left of invalid bag info error if the bagging date is invalid in bag-info.txt") {
    val bagInfoString =
      s"""|External-Identifier: $randomExternalIdentifier
          |Source-Organization: $randomSourceOrganisation
          |Payload-Oxum: ${randomPayloadOxum.payloadBytes}.${randomPayloadOxum.numberOfPayloadFiles}
          |Bagging-Date: sdfkjghl""".stripMargin

    BagInfoParser.parseBagInfo(
      request,
      IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(
      InvalidBagInfo(request, List("Bagging-Date")))
  }

  it("returns a left of invalid bag info error if bag-info.txt is empty") {
    val bagInfoString = ""

    BagInfoParser.parseBagInfo(
      request,
      IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(
      InvalidBagInfo(
        request,
        List(
          "External-Identifier",
          "Source-Organization",
          "Payload-Oxum",
          "Bagging-Date")))
  }

}
