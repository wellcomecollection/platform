package uk.ac.wellcome.transformer.transformers.sierra

import com.twitter.inject.Logging
import scala.util.{Failure, Success}
import uk.ac.wellcome.models.{
  IdentifierSchemes,
  SourceIdentifier,
  UnidentifiedItem
}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.transformer.source.SierraItemData
import uk.ac.wellcome.utils.JsonUtil._

trait SierraItems extends Logging with SierraCheckDigits with SierraLocation {
  def extractItemData(
    sierraTransformable: SierraTransformable): List[SierraItemData] = {
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

  def transformItemData(sierraItemData: SierraItemData): UnidentifiedItem = {
    info(s"Attempting to transform ${sierraItemData.id}")
    UnidentifiedItem(
      sourceIdentifier = SourceIdentifier(
        identifierScheme = IdentifierSchemes.sierraSystemNumber,
        ontologyType = "Item",
        value = addCheckDigit(
          sierraItemData.id,
          recordType = SierraRecordTypes.items
        )
      ),
      identifiers = List(
        SourceIdentifier(
          identifierScheme = IdentifierSchemes.sierraSystemNumber,
          ontologyType = "Item",
          value = addCheckDigit(
            sierraItemData.id,
            recordType = SierraRecordTypes.items
          )
        ),
        SourceIdentifier(
          identifierScheme = IdentifierSchemes.sierraIdentifier,
          ontologyType = "Item",
          value = sierraItemData.id
        )
      ),
      locations = getLocation(sierraItemData).toList
    )
  }

  def getItems(
    sierraTransformable: SierraTransformable): List[UnidentifiedItem] = {
    extractItemData(sierraTransformable)
      .filterNot { _.deleted }
      .map(transformItemData)
  }
}
