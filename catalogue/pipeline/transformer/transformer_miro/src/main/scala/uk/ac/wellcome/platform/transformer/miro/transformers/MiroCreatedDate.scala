package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.models.work.internal.Period
import uk.ac.wellcome.platform.transformer.miro.source.MiroTransformableData

trait MiroCreatedDate {
  def getCreatedDate(miroData: MiroTransformableData,
                     miroId: String): Option[Period] =
    if (collectionIsV(miroId)) {
      miroData.artworkDate.map { Period }
    } else {
      None
    }

  private def collectionIsV(miroId: String) = miroId.startsWith("V")
}
