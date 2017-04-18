package uk.ac.wellcome.transformer.parsers

import com.amazonaws.services.dynamodbv2.model.Record
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.MiroTransformable
import uk.ac.wellcome.transformer.receive.RecordMap
import uk.ac.wellcome.transformer.utils.MiroRecordUtils

class MiroParserTest extends FunSpec with MiroRecordUtils with Matchers {

  it("should parse a record representing Miro Data into a Miro Data transformable") {
    val miroParser = new MiroParser

    val MiroID = "1234"
    val MiroCollection = "Images-A"
    val data = """{"image-title": "this is the image title"}"""
    val triedMiroTransformable = miroParser.extractTransformable(
      createValidMiroRecord(MiroID, MiroCollection, data))

    triedMiroTransformable.isSuccess shouldBe true
    triedMiroTransformable.get shouldBe a[MiroTransformable]
    triedMiroTransformable.get
      .asInstanceOf[MiroTransformable] shouldBe MiroTransformable(
      MiroID,
      MiroCollection,
      data)
  }

  private implicit def toRecordMap(calmRecord: Record): RecordMap = {
    RecordMap(calmRecord.getDynamodb.getNewImage)
  }
}
