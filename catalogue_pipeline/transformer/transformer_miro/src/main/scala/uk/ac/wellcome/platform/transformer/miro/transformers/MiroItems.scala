package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

trait MiroItems extends MiroLocations {
  def getItemsV1(miroRecord: MiroRecord): List[Identifiable[Item]] =
    List(
      Identifiable(
        sourceIdentifier = SourceIdentifier(
          identifierType = IdentifierType("miro-image-number"),
          ontologyType = "Item",
          value = miroRecord.imageNumber
        ),
        agent = Item(
          locations = getLocations(miroRecord)
        )
      )
    )

  def getItems(miroRecord: MiroRecord): List[Unidentifiable[Item]] =
    List(Unidentifiable(Item(locations = getLocations(miroRecord))))
}
