package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.models.work.internal.{
  DigitalLocation,
  Location,
  LocationType
}
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

trait MiroThumbnail extends MiroImageApiURL with MiroLicenses {
  def getThumbnail(miroRecord: MiroRecord): Location =
    DigitalLocation(
      locationType = LocationType("thumbnail-image"),
      url = buildImageApiURL(
        miroId = miroRecord.imageNumber,
        templateName = "thumbnail"),
      license = Some(
        chooseLicense(
          miroId = miroRecord.imageNumber,
          maybeUseRestrictions = miroRecord.useRestrictions
        )
      )
    )
}
