package uk.ac.wellcome.transformer.parsers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.CalmTransformable
import uk.ac.wellcome.transformer.utils.TransformableSQSMessageUtils

class CalmParserTest
    extends FunSpec
    with TransformableSQSMessageUtils
    with Matchers {

  val RecordID = "abcdef"
  val RecordType = "collection"
  val RefNo = "AB/CD/12"
  val AltRefNo = "AB/CD/12"
  val data = """{"foo": ["bar"], "AccessStatus": ["TopSekrit"]}"""
  val calmRecord =
    createValidCalmTramsformableJson(RecordID,
                                     RecordType,
                                     AltRefNo,
                                     RefNo,
                                     data)

  it("should parse a record into a calm case class") {
    val calmParser = new TransformableParser
    val triedCalmTransformable =
      calmParser.extractTransformable(calmRecord)

    triedCalmTransformable.isSuccess should be(true)
    triedCalmTransformable.get shouldBe a[CalmTransformable]
    triedCalmTransformable.get
      .asInstanceOf[CalmTransformable] shouldBe CalmTransformable(RecordID,
                                                                  RecordType,
                                                                  AltRefNo,
                                                                  RefNo,
                                                                  data)
  }

  it("should return a failed try if it's unable to parse the message") {
    val calmParser = new TransformableParser

    val triedCalmTransformable =
      calmParser.extractTransformable(createInvalidJson)

    triedCalmTransformable.isFailure should be(true)
  }
}
