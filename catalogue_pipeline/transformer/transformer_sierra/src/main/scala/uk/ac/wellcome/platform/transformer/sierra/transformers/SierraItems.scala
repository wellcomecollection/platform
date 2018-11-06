package uk.ac.wellcome.platform.transformer.sierra.transformers

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibNumber,
  SierraItemNumber
}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.source.{
  SierraBibData,
  SierraItemData
}

trait SierraItems extends Logging with SierraLocation {
  def getItems(bibId: SierraBibNumber,
               bibData: SierraBibData,
               itemDataMap: Map[SierraItemNumber, SierraItemData])
    : List[MaybeDisplayable[Item]] = {
    val physicalItems = getPhysicalItems(itemDataMap)
    val maybeDigitalItem = getDigitalItem(bibId = bibId, bibData = bibData)

    // If we have a digital Item and a *single* physical Item, we know
    // the digital Item is the digitised version of the physical Item, so
    // we can combine their locations and present a single Item.
    //
    // If we have a digital Item and *multiple* physical Items, we don't
    // know which physical Item was digitised, so we leave them as
    // separate Items.
    //
    (physicalItems, maybeDigitalItem) match {
      case (Seq(physicalItem), Some(digitalItem)) =>
        List(
          physicalItem.copy(
            agent = physicalItem.agent.copy(
              locations = physicalItem.agent.locations ++ digitalItem.agent.locations
            )
          )
        )
      case _ => physicalItems ++ List(maybeDigitalItem).flatten
    }
  }

  private def transformItemData(
    itemId: SierraItemNumber,
    itemData: SierraItemData): Identifiable[Item] = {
    debug(s"Attempting to transform $itemId")
    Identifiable(
      sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Item",
        value = itemId.withCheckDigit
      ),
      otherIdentifiers = List(
        SourceIdentifier(
          identifierType = IdentifierType("sierra-identifier"),
          ontologyType = "Item",
          value = itemId.withoutCheckDigit
        )
      ),
      agent = Item(
        locations = getPhysicalLocation(itemData).toList
      )
    )
  }

  private def getPhysicalItems(
    sierraItemDataMap: Map[SierraItemNumber, SierraItemData])
    : List[Identifiable[Item]] =
    sierraItemDataMap
      .filterNot {
        case (_: SierraItemNumber, itemData: SierraItemData) => itemData.deleted
      }
      .map {
        case (itemId: SierraItemNumber, itemData: SierraItemData) =>
          transformItemData(
            itemId = itemId,
            itemData = itemData
          )
      }
      .toList

  /** Add digital items to a work.
    *
    * We can add digital items if there's a "dlnk" location in the
    * "locations" field of the bib record.  This is manually added by
    * Branwen when a record has been digitised, and is about to appear
    * on the website.
    *
    * Note: We can work out if a library record has a digitised version
    * from the METS files -- when we have those in the pipeline, we can do
    * away with this code.
    *
    */
  private def getDigitalItem(
    bibId: SierraBibNumber,
    bibData: SierraBibData): Option[Unidentifiable[Item]] = {
    val hasDlnkLocation = bibData.locations match {
      case Some(locations) => locations.map { _.code }.contains("dlnk")
      case None            => false
    }

    if (hasDlnkLocation) {
      Some(
        Unidentifiable(
          agent = Item(
            locations = List(getDigitalLocation(bibId.withCheckDigit))
          )
        )
      )
    } else {
      None
    }
  }
}
