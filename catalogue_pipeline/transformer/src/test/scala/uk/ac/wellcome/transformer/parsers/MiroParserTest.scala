package uk.ac.wellcome.transformer.parsers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.miro.MiroTransformable
import uk.ac.wellcome.transformer.transformers.MiroTransformableWrapper
import uk.ac.wellcome.transformer.utils.TransformableSQSMessageUtils

class MiroParserTest
    extends FunSpec
    with TransformableSQSMessageUtils
    with Matchers
    with MiroTransformableWrapper {
  it(
    "should parse a record representing Miro Data into a Miro Data transformable") {
    val miroParser = new MiroParser

    val MiroID = "1234"
    val MiroCollection = "Images-A"
    val data = buildJSONForWork(""""image_title": "this is the image title"""")
    val sQSMessage = createValidMiroRecord(MiroID, MiroCollection, data)
    val triedMiroTransformable = miroParser.extractTransformable(sQSMessage)

    triedMiroTransformable.isSuccess shouldBe true
    triedMiroTransformable.get shouldBe a[MiroTransformable]
    triedMiroTransformable.get
      .asInstanceOf[MiroTransformable] shouldBe MiroTransformable(
      MiroID,
      MiroCollection,
      data)
  }
}
