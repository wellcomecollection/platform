package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.models.work.internal.{DigitalLocation, LocationType}
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

trait MiroLocations
    extends MiroImageApiURL
    with MiroLicenses
    with MiroContributorCodes {

  def getLocations(miroRecord: MiroRecord): List[DigitalLocation] =
    List(
      DigitalLocation(
        locationType = LocationType("iiif-image"),
        url = buildImageApiURL(
          miroId = miroRecord.imageNumber,
          templateName = "info"
        ),
        credit = getCredit(miroRecord),
        license = Some(
          chooseLicense(
            miroId = miroRecord.imageNumber,
            maybeUseRestrictions = miroRecord.useRestrictions
          )
        )
      )
    )

  /** Image credits in MIRO could be set in two ways:
    *
    *    - using the image_credit_line, which is per-image
    *    - using the image_source_code, which falls back to a contributor-level
    *      credit line
    *
    * We prefer the per-image credit line, but use the contributor-level credit
    * if unavailable.
    */
  private def getCredit(miroRecord: MiroRecord): Option[String] = {
    miroRecord.creditLine match {

      // Some of the credit lines are inconsistent or use old names for
      // Wellcome, so we do a bunch of replacements and trimming to tidy
      // them up.
      case Some(line) =>
        Some(line
          .replaceAll(
            "Adrian Wressell, Heart of England NHSFT",
            "Adrian Wressell, Heart of England NHS FT")
          .replaceAll(
            "Andrew Dilley,Jane Greening & Bruce Lynn",
            "Andrew Dilley, Jane Greening & Bruce Lynn")
          .replaceAll(
            "Andrew Dilley,Nicola DeLeon & Bruce Lynn",
            "Andrew Dilley, Nicola De Leon & Bruce Lynn")
          .replaceAll(
            "Ashley Prytherch, Royal Surrey County Hospital NHS Foundation Trust",
            "Ashley Prytherch, Royal Surrey County Hospital NHS FT")
          .replaceAll(
            "David Gregory & Debbie Marshall",
            "David Gregory and Debbie Marshall")
          .replaceAll(
            "David Gregory&Debbie Marshall",
            "David Gregory and Debbie Marshall")
          .replaceAll("Geraldine Thompson.", "Geraldine Thompson")
          .replaceAll("John & Penny Hubley.", "John & Penny Hubley")
          .replaceAll(
            "oyal Army Medical Corps Muniment Collection, Wellcome Images",
            "Royal Army Medical Corps Muniment Collection, Wellcome Collection")
          .replaceAll("Science Museum London", "Science Museum, London")
          .replaceAll("The Wellcome Library, London", "Wellcome Collection")
          .replaceAll("Wellcome Library, London", "Wellcome Collection")
          .replaceAll("Wellcome Libary, London", "Wellcome Collection")
          .replaceAll("Wellcome LIbrary, London", "Wellcome Collection")
          .replaceAll("Wellcome Images", "Wellcome Collection")
          .replaceAll("The Wellcome Library", "Wellcome Collection")
          .replaceAll("Wellcome Library", "Wellcome Collection")
          .replaceAll("Wellcome Collection London", "Wellcome Collection")
          .replaceAll("Wellcome Collection, Londn", "Wellcome Collection")
          .replaceAll("Wellcome Trust", "Wellcome Collection")
          .replaceAll("'Wellcome Collection'", "Wellcome Collection"))

      // Otherwise we carry through the contributor codes, which have
      // already been edited for consistency.
      case None =>
        miroRecord.sourceCode match {
          case Some(code) =>
            lookupContributorCode(miroId = miroRecord.imageNumber, code = code)
          case None => None
        }
    }
  }
}
