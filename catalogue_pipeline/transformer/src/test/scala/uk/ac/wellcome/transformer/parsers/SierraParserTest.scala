package uk.ac.wellcome.transformer.parsers

import java.time.Instant

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord
}
import uk.ac.wellcome.models.transformable.{SierraTransformable, Transformable}
import uk.ac.wellcome.transformer.utils.TransformableSQSMessageUtils
import uk.ac.wellcome.utils.JsonUtil

import scala.util.Try

class SierraParserTest
    extends FunSpec
    with TransformableSQSMessageUtils
    with Matchers {
  it("should parse a sierra merged record") {
    val id = "000"
    val title = "A flock of flanged flamingos in France"
    val lastModifiedDate =
      Instant.ofEpochSecond(Instant.now().getEpochSecond())

    val data =
      s"""
         |{
         | "id": "$id",
         | "title": "$title",
         | "varFields": []
         |}
      """.stripMargin

    val sqsMessage =
      createValidSierraBibSQSMessage(id, title, lastModifiedDate)

    val sierraParser = new TransformableParser

    val triedSierraTransformable =
      sierraParser.extractTransformable(sqsMessage)

    triedSierraTransformable.isSuccess shouldBe true
    triedSierraTransformable.get shouldBe a[SierraTransformable]

    val expectedRecord = SierraTransformable(
      id = id,
      maybeBibData = Some(SierraBibRecord(id, data, lastModifiedDate))
    )

    val actualRecord =
      triedSierraTransformable.get.asInstanceOf[SierraTransformable]

    actualRecord shouldEqual expectedRecord
  }
}
