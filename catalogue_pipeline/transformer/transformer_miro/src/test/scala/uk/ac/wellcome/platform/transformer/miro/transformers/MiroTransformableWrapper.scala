package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{Matchers, Suite}
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.models.work.internal.{
  TransformedBaseWork,
  UnidentifiedWork
}
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException
import uk.ac.wellcome.platform.transformer.miro.MiroTransformableTransformer

import scala.util.Try

/** MiroTransformable looks for several fields in the source JSON -- if they're
  *  missing or have the wrong values, it rejects the record.
  *
  *  This trait provides a single method `transform()` which adds the necessary
  *  fields before transformation, allowing tests to focus on only the fields
  *  that are interesting for that test.
  */
trait MiroTransformableWrapper extends Matchers { this: Suite =>

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
    data: String = "",
    MiroID: String = "M0000001",
    MiroCollection: String = "TestCollection"
  ): UnidentifiedWork = {
    val miroTransformable = MiroTransformable(
      sourceId = MiroID,
      MiroCollection = MiroCollection,
      data = buildJSONForWork(data)
    )

    transformToWork(miroTransformable).asInstanceOf[UnidentifiedWork]
  }

  def assertTransformWorkFails(
    data: String,
    MiroID: String = "M0000001",
    MiroCollection: String = "TestCollection"
  ) = {
    val miroTransformable = MiroTransformable(
      sourceId = MiroID,
      MiroCollection = MiroCollection,
      data = buildJSONForWork(data)
    )

    assertTransformToWorkFails(miroTransformable)
  }

  def transformToWork(transformable: MiroTransformable): TransformedBaseWork = {
    val triedWork: Try[TransformedBaseWork] =
      transformer.transform(transformable, version = 1)

    if (triedWork.isFailure) {
      triedWork.failed.get.printStackTrace()
      println(
        triedWork.failed.get.asInstanceOf[TransformerException].e.getMessage)
    }

    triedWork.isSuccess shouldBe true
    triedWork.get
  }

  def assertTransformToWorkFails(transformable: MiroTransformable): Unit = {
    transformer
      .transform(transformable, version = 1)
      .isSuccess shouldBe false
  }

}
