package uk.ac.wellcome.transformer.parsers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.MergedSierraRecord
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.transformer.utils.TransformableSQSMessageUtils
import uk.ac.wellcome.utils.JsonUtil

import scala.util.Try

class SierraParserTest
    extends FunSpec
    with TransformableSQSMessageUtils
    with Matchers {
  it("should parse a sierra merged record") {
    val id = "000"
    val sqsMessage = createValidEmptySierraBibSQSMessage(id)

    val sierraParser = new SierraParser

    val triedSierraTransformable =
      sierraParser.extractTransformable(sqsMessage)

    triedSierraTransformable.isSuccess shouldBe true
    triedSierraTransformable.get shouldBe a[MergedSierraRecord]
    val actualRecord =
      triedSierraTransformable.get.asInstanceOf[MergedSierraRecord]
    actualRecord.id shouldEqual id
  }
}
