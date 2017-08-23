package uk.ac.wellcome.models.transformable.miro

import uk.ac.wellcome.models.transformable.{
  FieldIssues,
  ShouldNotTransformException
}

trait MiroTransformChecks {
  /* XML tags refer to fields within the Miro XML dumps.
   * If the <image_cleared> or <image_copyright_cleared> fields are
   * missing or don't have have the value 'Y', then we shouldn't expose
   * this image in the public API.
   *
   * See https://github.com/wellcometrust/platform-api/issues/356
   */
  private def checkCopyrightCleared(
    data: MiroTransformableData): List[FieldIssues] = {
    val generalCleared = if (data.cleared.getOrElse("N") != "Y") {
      List(FieldIssues("image_cleared", data.cleared))
    } else Nil

    val copyrightCleared = if (data.copyrightCleared.getOrElse("N") != "Y") {
      List(FieldIssues("image_cleared", data.cleared))
    } else Nil

    generalCleared ++ copyrightCleared
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
  private def checkImageExists(
    data: MiroTransformableData): List[FieldIssues] =
    if (data.techFileSize.getOrElse(List[String]()).isEmpty) {
      List(
        FieldIssues(
          "image_tech_file_size",
          data.techFileSize,
          Some(
            "Missing image_tech_file_size means there is likely no underlying image")))
    } else Nil

  private def checkLicense(data: MiroTransformableData): List[FieldIssues] =
    data.useRestrictions match {
      case None =>
        throw ShouldNotTransformException(
          List(
            FieldIssues("image_use_restrictions",
                        data.useRestrictions,
                        Some("No value provided for image_use_restrictions?"))
          ))

      case Some("CC-0") | Some("CC-BY") | Some("CC-BY-NC") | Some(
            "CC-BY-NC-ND") =>
        Nil

      // Any images with this label are explicitly withheld from the API.
      case Some("See Related Images Tab for Higher Res Available") => {
        List(
          FieldIssues("image_use_restrictions",
                      data.useRestrictions,
                      Some("Images with this license are explicitly excluded"))
        )
      }

      // These fields are labelled "[Investigate further]" in Christy's
      // document, so for now we exclude them.
      case Some("Do not use") | Some("Not for external use") | Some(
            "See Copyright Information") | Some("Suppressed from WI site") => {
        List(
          FieldIssues(
            "image_use_restrictions",
            data.useRestrictions,
            Some(
              "Images with this license need more investigation before showing in the API"))
        )
      }

      case _ =>
        List(
          FieldIssues("image_use_restrictions",
                      data.useRestrictions,
                      Some("This license type is unrecognised"))
        )
    }

  val checks: List[(MiroTransformableData) => List[FieldIssues]] = List(
    checkCopyrightCleared,
    checkImageExists,
    checkLicense
  )
}
