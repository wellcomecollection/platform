package uk.ac.wellcome.platform.transformer.transformers

import org.scalatest.{Matchers, Suite}
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.models.work.internal.UnidentifiedWork

/** MiroTransformable looks for several fields in the source JSON -- if they're
  *  missing or have the wrong values, it rejects the record.
  *
  *  This trait provides a single method `transform()` which adds the necessary
  *  fields before transformation, allowing tests to focus on only the fields
  *  that are interesting for that test.
  */
trait MiroTransformableWrapper
    extends Matchers
    with TransformableTestBase[MiroTransformable] { this: Suite =>

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
}
