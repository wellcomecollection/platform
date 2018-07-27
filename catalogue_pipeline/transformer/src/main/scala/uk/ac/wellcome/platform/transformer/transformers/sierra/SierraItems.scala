package uk.ac.wellcome.platform.transformer.transformers.sierra

import grizzled.slf4j.Logging
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemNumber
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.{SierraBibData, SierraItemData, SierraMaterialType}
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.{Failure, Success}

trait SierraItems extends Logging with SierraLocation {
  def extractItemData(
    sierraTransformable: SierraTransformable): Map[SierraItemNumber, SierraItemData] =
    sierraTransformable.itemRecords
      .map { case (id, itemRecord) => (id, itemRecord.data) }
      .map {
        case (id, jsonString) =>
          fromJson[SierraItemData](jsonString) match {
            case Success(data) => id -> data
            case Failure(_) =>
              throw GracefulFailureException(
                new RuntimeException(
                  s"Unable to parse item data for $id as JSON: <<$jsonString>>"
                ))
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
