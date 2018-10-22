package uk.ac.wellcome.platform.archive.archivist.bag
import org.apache.commons.io.IOUtils
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.archivist.models.IngestRequestContextGenerators
import uk.ac.wellcome.platform.archive.archivist.models.errors.InvalidBagInfo
import uk.ac.wellcome.platform.archive.common.fixtures.BagIt
import uk.ac.wellcome.platform.archive.common.models.{BagInfo, ExternalIdentifier}

class BagInfoExtractorTest extends FunSpec with BagIt with Matchers with IngestRequestContextGenerators{
  it("extracts a BagInfo object from a valid bagInfo file") {
    val externalIdentifier = ExternalIdentifier("b1234567x")
    val bagInfoString = bagInfoFileContents(externalIdentifier)

    BagInfoExtractor.extractBagInfo(createIngestBagRequest,IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Right(BagInfo(externalIdentifier))
  }

  it("returns a left of invalid bag info error if there is no external identifier in bag-info.txt"){
    val bagInfoString = ""

    val request = createIngestBagRequest
    BagInfoExtractor.extractBagInfo(request,IOUtils.toInputStream(bagInfoString, "UTF-8")) shouldBe Left(InvalidBagInfo(request))
  }

}
