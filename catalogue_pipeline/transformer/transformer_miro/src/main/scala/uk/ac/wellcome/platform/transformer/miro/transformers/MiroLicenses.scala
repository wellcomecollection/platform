package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.miro.exceptions.ShouldNotTransformException

trait MiroLicenses {

  /** If the image has a non-empty image_use_restrictions field, choose which
    *  license (if any) we're going to assign to the thumbnail for this work.
    *
    *  The mappings in this function are based on a document provided by
    *  Christy Henshaw (MIRO drop-downs.docx).  There are still some gaps in
    *  that, we'll have to come back and update this code later.
    *
    *  For now, this mapping only covers use restrictions seen in the
    *  V collection.  We'll need to extend this for other licenses later.
    *
    *  TODO: Expand this mapping to cover all of MIRO.
    *  TODO: Update these mappings based on the final version of Christy's
    *        document.
    */
  def chooseLicense(miroId: String,
                    maybeUseRestrictions: Option[String]): License =
    maybeUseRestrictions match {

      // These images need more data.
      case None =>
        throw new ShouldNotTransformException(
          s"Image $miroId has no usage restriction specified!")

      case Some(useRestrictions) =>
        useRestrictions match {

          // Certain strings map directly onto license types
          case "CC-0"         => License_CC0
          case "CC-BY"        => License_CCBY
          case "CC-BY-NC"     => License_CCBYNC
          case "CC-BY-NC-ND"  => License_CCBYNCND
          case "PDM"          => License_PDM
          case "In copyright" => License_CopyrightNotCleared

          // These mappings are defined in Christy's document
          case "Academics" => License_CCBYNC

          // These images should really be removed entirely and sent to something
          // like Tandem Vault, but we have seen some of these strings in the
          // catalogue data -- for now, explicitly mark these as "do not transform"
          // so they don't end up on the DLQ.
          case "Do not use" =>
            throw new ShouldNotTransformException(
              s"Image $miroId has usage restriction 'Do not use'")
          case "Image withdrawn, see notes" =>
            throw new ShouldNotTransformException(
              s"Image $miroId has usage restriction 'Image withdrawn'")
        }
    }

}
