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

trait SierraItems extends Logging with SierraLocation {
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
        IdentifierSchemes.sierraSystemNumber,
        sierraItemData.id
      ),
      identifiers = List(
        SourceIdentifier(
          identifierScheme = IdentifierSchemes.sierraSystemNumber,
          sierraItemData.id
        )
      ),
      locations = getLocation(sierraItemData).toList,
      visible = !sierraItemData.deleted
    )
  }

  def getItems(
    sierraTransformable: SierraTransformable): List[UnidentifiedItem] = {
    extractItemData(sierraTransformable)
      .map(transformItemData)
  }
}
