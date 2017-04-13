package uk.ac.wellcome.transformer.parsers

import com.amazonaws.services.dynamodbv2.model.Record
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.CalmTransformable
import uk.ac.wellcome.transformer.receive.RecordMap
import uk.ac.wellcome.transformer.utils.CalmRecordUtils

class CalmParserTest extends FunSpec with CalmRecordUtils with Matchers {

  val RecordID = "abcdef"
  val RecordType = "collection"
  val RefNo = "AB/CD/12"
  val AltRefNo = "AB/CD/12"
  val data = """{"foo": ["bar"], "AccessStatus": ["TopSekrit"]}"""
  val calmRecord =
    createValidCalmRecord(RecordID, RecordType, AltRefNo, RefNo, data)

  it("should parse a record into a calm case class") {
    val calmParser = new CalmParser
    val triedCalmTransformable =
      calmParser.extractTransformable(toRecordMap(calmRecord))

    triedCalmTransformable.isSuccess should be(true)
    triedCalmTransformable.get shouldBe a[CalmTransformable]
    triedCalmTransformable.get
      .asInstanceOf[CalmTransformable] shouldBe CalmTransformable(RecordID,
                                                                  RecordType,
                                                                  AltRefNo,
                                                                  RefNo,
                                                                  data)
  }

  it("should return a failed try if it's unable to transform the parsed record") {
    val calmParser = new CalmParser

    val triedCalmTransformable =
      calmParser.extractTransformable(toRecordMap(createInvalidRecord))

    triedCalmTransformable.isFailure should be(true)
  }

  private def toRecordMap(calmRecord: Record) = {
    RecordMap(calmRecord.getDynamodb.getNewImage)
  }
}
