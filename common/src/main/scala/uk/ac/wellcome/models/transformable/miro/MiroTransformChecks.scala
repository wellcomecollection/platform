package uk.ac.wellcome.models.transformable.miro

import uk.ac.wellcome.models.transformable.{
  FieldIssues,
  ShouldNotTransformException
}

import scala.util.Try

trait MiroTransformChecks {
  type MiroTry = Try[MiroTransformableData]

  /* XML tags refer to fields within the Miro XML dumps.
   * If the <image_cleared> or <image_copyright_cleared> fields are
   * missing or don't have have the value 'Y', then we shouldn't expose
   * this image in the public API.
   *
   * See https://github.com/wellcometrust/platform-api/issues/356
   */
  private def checkCopyrightCleared(data: MiroTransformableData): MiroTry =
    Try {
      val generalCleared = if (data.cleared.getOrElse("N") != "Y") {
        List(FieldIssues("image_cleared", data.cleared))
      } else Nil

      val copyrightCleared = if (data.copyrightCleared.getOrElse("N") != "Y") {
        List(FieldIssues("image_cleared", data.cleared))
      } else Nil

      val issues = generalCleared ++ copyrightCleared

      if (issues.nonEmpty) {
        throw ShouldNotTransformException(issues)
      }

      data
    }

  /* There are a bunch of <image_tech_*> fields that refer to the
   * underlying image file.  If these are empty, there isn't actually a
   * file to retrieve, which breaks the Collection site.  Sometimes this is
   * a "glue" record that refers to multiple images.  e.g. V0011212ETL
   *
   * Eventually it might be nice to collate these -- have all the images
   * in the same API result, but for now, we just exclude them from
   * the API.  They aren't useful for testing image search.
   */
  private def checkImageExists(data: MiroTransformableData): MiroTry = Try {
    if (data.techFileSize.getOrElse(List[String]()).isEmpty) {
      throw ShouldNotTransformException(List(
        FieldIssues(
          "image_tech_file_size",
          data.techFileSize,
          Some(
            "Missing image_tech_file_size means there is no underlying image"))
      ))
    }

    data
  }

  private def checkLicense(data: MiroTransformableData): MiroTry = Try {
    data.useRestrictions match {
      case None =>
        throw ShouldNotTransformException(
          List(
            FieldIssues("image_tech_file_size",
                        data.techFileSize,
                        Some("No value provided for image_use_restrictions?"))
          ))

      case Some("CC-0") => data
      case Some("CC-BY") => data
      case Some("CC-BY-NC") => data
      case Some("CC-BY-NC-ND") => data

      // Any images with this label are explicitly withheld from the API.
      case Some("See Related Images Tab for Higher Res Available") => {
        throw ShouldNotTransformException(
          List(
            FieldIssues(
              "image_use_restrictions",
              data.useRestrictions,
              Some("Images with this license are explicitly excluded"))
          ))
      }

      // These fields are labelled "[Investigate further]" in Christy's
      // document, so for now we exclude them.
      case Some(
          "Do not use" | "Not for external use" | "See Copyright Information" |
          "Suppressed from WI site") => {
        throw ShouldNotTransformException(
          List(
            FieldIssues(
              "image_use_restrictions",
              data.useRestrictions,
              Some(
                "Images with this license need more investigation before showing in the API"))
          ))
      }

      case _ =>
        throw ShouldNotTransformException(
          List(
            FieldIssues("image_use_restrictions",
                        data.useRestrictions,
                        Some("This license type is unrecognised"))
          ))
    }

    data
  }

  val checks: List[(MiroTransformableData) => MiroTry] = List(
    checkCopyrightCleared,
    checkImageExists,
    checkLicense
  )
}
