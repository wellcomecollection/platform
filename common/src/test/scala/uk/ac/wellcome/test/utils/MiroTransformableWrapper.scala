package uk.ac.wellcome.test.utils

import org.scalatest.{Matchers, Suite}
import scala.util.Try

import uk.ac.wellcome.models.{MiroTransformable, Work}

/** MiroTransformable looks for several fields in the source JSON -- if they're
 *  missing or have the wrong values, it rejects the record.
 *
 *  This trait provides a single method `transform()` which adds the necessary
 *  fields before transformation, allowing tests to focus on only the fields
 *  that are interesting for that test.
 */
trait MiroTransformableWrapper extends Matchers { this: Suite =>

  def transformWork(
    data: String,
    MiroID: String = "M0000001",
    MiroCollection: String = "TestCollection"
  ): Work = {
    val miroTransformable = MiroTransformable(
      MiroID = MiroID,
      MiroCollection = MiroCollection,
      data = s"""{
        "image_cleared": "Y",
        "image_copyright_cleared": "Y",
        "image_tech_file_size": ["1000000"],
        "image_use_restrictions": "CC-BY",
        $data
      }"""
    )
    miroTransformable.transform.isSuccess shouldBe true
    miroTransformable.transform.get
  }

  def assertTransformWorkFails(
      data: String,
      miroID: String = "M0000001",
      miroCollection: String = "TestCollection"
    ) = {
      val miroTransformable = MiroTransformable(
        MiroID = miroID,
        MiroCollection = miroCollection,
        data = data
      )

      miroTransformable.transform.isSuccess shouldBe false
    }
}
