package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.models.work.internal.{
  DigitalLocation,
  Location,
  LocationType
}
import uk.ac.wellcome.platform.transformer.miro.source.MiroTransformableData

trait MiroThumbnail extends MiroImageApiURL with MiroLicenses {
  def getThumbnail(miroData: MiroTransformableData, miroId: String): Location =
    DigitalLocation(
      locationType = LocationType("thumbnail-image"),
      url = buildImageApiURL(miroId, templateName = "thumbnail"),
      license = Some(chooseLicense(miroId, miroData.useRestrictions))
    )
}
