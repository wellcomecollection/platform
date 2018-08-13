package uk.ac.wellcome.platform.transformer.transformers.sierra

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemNumber
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.{
  SierraBibData,
  SierraItemData
}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException

import scala.util.{Failure, Success}

trait SierraItems extends Logging with SierraLocation {
  def extractItemData(sierraTransformable: SierraTransformable)
    : Map[SierraItemNumber, SierraItemData] =
    sierraTransformable.itemRecords
      .map { case (id, itemRecord) => (id, itemRecord.data) }
      .map {
        case (id, jsonString) =>
          fromJson[SierraItemData](jsonString) match {
            case Success(data) => id -> data
            case Failure(_) =>
              throw TransformerException(
                s"Unable to parse item data for $id as JSON: <<$jsonString>>")
          }
      }

  def transformItemData(itemId: SierraItemNumber,
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

  def getPhysicalItems(
    sierraTransformable: SierraTransformable): List[Identifiable[Item]] =
    extractItemData(sierraTransformable)
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

  private def getDigitalItem(
    sourceIdentifier: SourceIdentifier): Unidentifiable[Item] = {
    Unidentifiable(
      agent = Item(
        locations = List(getDigitalLocation(sourceIdentifier.value))
      )
    )
  }

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
  def getDigitalItems(
    sourceIdentifier: SourceIdentifier,
    sierraBibData: SierraBibData): List[Unidentifiable[Item]] = {
    val hasDlnkLocation = sierraBibData.locations match {
      case Some(locations) => locations.map { _.code }.contains("dlnk")
      case None            => false
    }

    if (hasDlnkLocation) {
      List(getDigitalItem(sourceIdentifier))
    } else {
      List()
    }
  }
}
