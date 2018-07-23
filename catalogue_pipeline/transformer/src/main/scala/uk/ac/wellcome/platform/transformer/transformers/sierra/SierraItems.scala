package uk.ac.wellcome.platform.transformer.transformers.sierra

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.{
  SierraRecordNumbers,
  SierraRecordTypes
}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.{SierraBibData, SierraItemData, SierraMaterialType}
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.{Failure, Success}

trait SierraItems extends Logging with SierraLocation {
  def extractItemData(sierraTransformable: SierraTransformable): List[SierraItemData] = {
    sierraTransformable.itemData.values
      .map { _.data }
      .map { jsonString =>
        fromJson[SierraItemData](jsonString) match {
          case Success(d) => Some(d)
          case Failure(e) => {
            error(s"Failed to parse item!", e)
            None
          }
        }
      }
      .toList
      .flatten
  }

  def transformItemData(sierraItemData: SierraItemData): Identifiable[Item] = {
    debug(s"Attempting to transform ${sierraItemData.id}")
    Identifiable(
      sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Item",
        value = SierraRecordNumbers.addCheckDigit(
          sierraItemData.id,
          recordType = SierraRecordTypes.items
        )
      ),
      otherIdentifiers = List(
        SourceIdentifier(
          identifierType = IdentifierType("sierra-identifier"),
          ontologyType = "Item",
          value = sierraItemData.id
        )
      ),
      agent = Item(
        locations =
          getPhysicalLocation(sierraItemData).toList
      )
    )
  }

  def getPhysicalItems(sierraTransformable: SierraTransformable): List[Identifiable[Item]] = {
    extractItemData(sierraTransformable)
      .filterNot { _.deleted }
      .map(transformItemData)
  }

  def getDigitalItem(sourceIdentifier: SourceIdentifier): Identifiable[Item] = {
    Identifiable(
      sourceIdentifier = sourceIdentifier,
      agent = Item(
        locations = List(getDigitalLocation(sourceIdentifier.value))
      )
    )
  }

  def getDigitalItems(sourceIdentifier: SourceIdentifier, sierraBibData: SierraBibData): List[Identifiable[Item]] = {
    sierraBibData.materialType match {
      case Some(SierraMaterialType("v", "E-books"))  => List(getDigitalItem(sourceIdentifier))
      case _ => List.empty
    }
  }

}
