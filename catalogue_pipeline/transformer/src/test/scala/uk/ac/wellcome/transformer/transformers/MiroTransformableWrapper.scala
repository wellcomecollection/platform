package uk.ac.wellcome.transformer.transformers

import org.scalatest.{Matchers, Suite}
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.transformable.MiroTransformable

/** MiroTransformable looks for several fields in the source JSON -- if they're
  *  missing or have the wrong values, it rejects the record.
  *
  *  This trait provides a single method `transform()` which adds the necessary
  *  fields before transformation, allowing tests to focus on only the fields
  *  that are interesting for that test.
  */
trait MiroTransformableWrapper extends Matchers { this: Suite =>

  val transformer = new MiroTransformableTransformer
  def buildJSONForWork(extraData: String): String =
    s"""{
        "image_cleared": "Y",
        "image_copyright_cleared": "Y",
        "image_tech_file_size": ["1000000"],
        "image_use_restrictions": "CC-BY",
        $extraData
      }"""

  def transformWork(
    data: String,
    MiroID: String = "M0000001",
    MiroCollection: String = "TestCollection"
  ): Work = {
    val miroTransformable = MiroTransformable(
      sourceId = MiroID,
      MiroCollection = MiroCollection,
      data = buildJSONForWork(data)
    )

    val triedMaybeWork = transformer.transform(miroTransformable)
    if (triedMaybeWork.isFailure) triedMaybeWork.failed.get.printStackTrace()
    triedMaybeWork.isSuccess shouldBe true
    triedMaybeWork.get.get
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
    transformer.transform(miroTransformable).isSuccess shouldBe false
  }
}
