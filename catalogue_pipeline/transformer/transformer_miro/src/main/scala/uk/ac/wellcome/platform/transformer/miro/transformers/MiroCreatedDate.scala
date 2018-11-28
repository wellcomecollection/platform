package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.models.work.internal.Period
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

trait MiroCreatedDate {
  def getCreatedDate(miroRecord: MiroRecord): Option[Period] =
    if (collectionIsV(miroRecord.imageNumber)) {
      miroRecord.artworkDate.map { date =>
        Period(label = date)
      }
    } else {
      None
    }

  private def collectionIsV(imageNumber: String): Boolean =
    imageNumber.startsWith("V")
}
