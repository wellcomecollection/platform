package uk.ac.wellcome.test.utils

import org.scalatest.Suite
import scala.util.Try

import uk.ac.wellcome.models.{MiroTransformable, Work}

/** MiroTransformable looks for several fields in the source JSON -- if they're
 *  missing or have the wrong values, it rejects the record.
 *
 *  This trait provides a single method `transform()` which adds the necessary
 *  fields before transformation, allowing tests to focus on only the fields
 *  that are interesting for that test.
 */
trait MiroTransformableWrapper { this: Suite =>

  def transformWork(
    data: String,
    MiroID: String = "M0000001",
    MiroCollection: String = "TestCollection"
  ): Try[Work] = {
    val miroTransformable = MiroTransformable(
      MiroID = MiroID,
      MiroCollection = MiroCollection,
      data = s"""{
        "image_cleared": "Y",
        "image_copyright_cleared": "Y",
        $data
      }"""
    )
    miroTransformable.transform
  }
}
