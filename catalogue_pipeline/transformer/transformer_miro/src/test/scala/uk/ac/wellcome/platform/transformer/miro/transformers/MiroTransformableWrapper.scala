package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{Assertion, Matchers, Suite}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.{
  TransformedBaseWork,
  UnidentifiedWork
}
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException
import uk.ac.wellcome.platform.transformer.miro.MiroTransformableTransformer
import uk.ac.wellcome.platform.transformer.miro.generators.MiroTransformableGenerators
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

import scala.util.Try

/** MiroTransformable looks for several fields in the source JSON -- if they're
  *  missing or have the wrong values, it rejects the record.
  *
  *  This trait provides a single method `transform()` which adds the necessary
  *  fields before transformation, allowing tests to focus on only the fields
  *  that are interesting for that test.
  */
trait MiroTransformableWrapper
    extends Matchers
    with MiroTransformableGenerators { this: Suite =>

  val transformer = new MiroTransformableTransformer
  def buildJSONForWork(extraData: String): String = {
    val baseData =
      """
        |"image_cleared": "Y",
        |        "image_copyright_cleared": "Y",
        |        "image_tech_file_size": ["1000000"],
        |        "image_use_restrictions": "CC-BY"
      """.stripMargin

    if (extraData.isEmpty) s"""{$baseData}"""
    else s"""{
        $baseData,
        $extraData
      }"""
  }

  def transformWork(
    miroId: String = "M0000001",
    data: String = ""
  ): UnidentifiedWork = {
    val miroRecord = fromJson[MiroRecord](buildJSONForWork(data)).get

    transformToWork(miroRecord).asInstanceOf[UnidentifiedWork]
  }

  def transformWork(miroRecord: MiroRecord): UnidentifiedWork =
    transformToWork(miroRecord).asInstanceOf[UnidentifiedWork]

  def assertTransformWorkFails(data: String): Assertion = {
    val miroTransformable = createMiroTransformableWith(
      data = buildJSONForWork(data)
    )

    transformer
      .transform(miroTransformable, version = 1)
      .isSuccess shouldBe false
  }

  private def transformToWork(miroRecord: MiroRecord): TransformedBaseWork = {
    val triedWork: Try[TransformedBaseWork] =
      transformer.transform(miroRecord, version = 1)

    if (triedWork.isFailure) {
      triedWork.failed.get.printStackTrace()
      println(
        triedWork.failed.get.asInstanceOf[TransformerException].e.getMessage)
    }

    triedWork.isSuccess shouldBe true
    triedWork.get
  }
}
