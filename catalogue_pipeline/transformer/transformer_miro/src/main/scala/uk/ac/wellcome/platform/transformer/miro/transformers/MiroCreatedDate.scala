package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.models.work.internal.Period
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

trait MiroCreatedDate {
  def getCreatedDate(miroRecord: MiroRecord,
                     miroId: String): Option[Period] =
    if (collectionIsV(miroId)) {
      miroRecord.artworkDate.map { Period }
    } else {
      None
    }

  private def collectionIsV(miroId: String) = miroId.startsWith("V")
}
