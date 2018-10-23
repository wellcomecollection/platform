package uk.ac.wellcome.platform.archive.archivist.bag
import org.apache.commons.io.IOUtils
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.archivist.models.IngestRequestContextGenerators
import uk.ac.wellcome.platform.archive.archivist.models.errors.InvalidBagInfo
import uk.ac.wellcome.platform.archive.common.fixtures.{BagIt, RandomThings}
import uk.ac.wellcome.platform.archive.common.models.BagInfo

class BagInfoExtractorTest extends FunSpec with BagIt with Matchers with IngestRequestContextGenerators with RandomThings{
  it("extracts a BagInfo object from a valid bagInfo file") {
    val externalIdentifier = randomExternalIdentifier
    val sourceOrganisation = randomSourceOrganisation
    val bagInfoString = bagInfoFileContents(externalIdentifier, sourceOrganisation)

    BagInfoExtractor.extractBagInfo(createIngestBagRequest,IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Right(BagInfo(externalIdentifier, sourceOrganisation))
  }

  it("returns a left of invalid bag info error if there is no external identifier in bag-info.txt"){
    val bagInfoString = s"Source-Organization: $randomSourceOrganisation"

    val request = createIngestBagRequest
    BagInfoExtractor.extractBagInfo(request,IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(InvalidBagInfo(request, List("External-Identifier")))
  }

  it("returns a left of invalid bag info error if there is no source organization in bag-info.txt"){
    val bagInfoString = s"External-Identifier: $randomExternalIdentifier"

    val request = createIngestBagRequest
    BagInfoExtractor.extractBagInfo(request,IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(InvalidBagInfo(request, List("Source-Organization")))
  }

  it("returns a left of invalid bag info error if bag-info.txt is empty"){
    val bagInfoString = ""

    val request = createIngestBagRequest
    BagInfoExtractor.extractBagInfo(request,IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(InvalidBagInfo(request, List("External-Identifier","Source-Organization")))
  }

}
