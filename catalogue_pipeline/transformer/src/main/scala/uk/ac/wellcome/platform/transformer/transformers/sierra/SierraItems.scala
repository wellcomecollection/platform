package uk.ac.wellcome.platform.transformer.transformers.sierra

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.{
  SierraRecordNumbers,
  SierraRecordTypes
}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.{
  SierraBibData,
  SierraItemData,
  SierraMaterialType
}
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.{Failure, Success}

trait SierraItems extends Logging with SierraLocation {
  def extractItemData(
    sierraTransformable: SierraTransformable): Map[String, SierraItemData] =
    sierraTransformable.itemData
      .map { case (id, itemRecord) => (id, itemRecord.data) }
      .map {
        case (id, jsonString) =>
          fromJson[SierraItemData](jsonString) match {
            case Success(data) => Some(id -> data)
            case Failure(e) => {
              error(s"Failed to parse item!", e)
              None
            }
          }
      }
      .flatten
      .toMap

  def transformItemData(itemId: String, itemData: SierraItemData): Identifiable[Item] = {
    debug(s"Attempting to transform $itemId")
    Identifiable(
      sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Item",
        value = SierraRecordNumbers.addCheckDigit(
          sierraId = itemId,
          recordType = SierraRecordTypes.items
        )
      ),
      otherIdentifiers = List(
        SourceIdentifier(
          identifierType = IdentifierType("sierra-identifier"),
          ontologyType = "Item",
          value = itemId
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
        case (_: String, itemData: SierraItemData) => itemData.deleted
      }
      .map {
        case (itemId: String, itemData: SierraItemData) =>
          transformItemData(
            itemId = itemId,
            itemData = itemData
          )
      }
      .toList

  def getDigitalItem(sourceIdentifier: SourceIdentifier): Identifiable[Item] = {
    Identifiable(
      sourceIdentifier = sourceIdentifier,
      agent = Item(
        locations = List(getDigitalLocation(sourceIdentifier.value))
      )
    )
  }

  def getDigitalItems(
    sourceIdentifier: SourceIdentifier,
    sierraBibData: SierraBibData): List[Identifiable[Item]] = {
    sierraBibData.materialType match {
      case Some(SierraMaterialType("v", "E-books")) =>
        List(getDigitalItem(sourceIdentifier))
      case _ => List.empty
    }
  }

}
