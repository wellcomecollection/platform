package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

trait MiroItems extends MiroLocations {

  def getItemsV1(miroRecord: MiroRecord,
                 miroId: String): List[Identifiable[Item]] =
    List(
      Identifiable(
        sourceIdentifier = SourceIdentifier(
          identifierType = IdentifierType("miro-image-number"),
          "Item",
          miroId),
        agent = Item(
          locations = getLocations(miroRecord, miroId)
        )
      ))

  def getItems(miroRecord: MiroRecord,
               miroId: String): List[Unidentifiable[Item]] =
    List(Unidentifiable(Item(locations = getLocations(miroRecord, miroId))))

}
