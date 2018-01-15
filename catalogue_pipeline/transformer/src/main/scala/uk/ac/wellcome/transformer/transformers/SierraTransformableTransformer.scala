package uk.ac.wellcome.transformer.transformers

import com.twitter.inject.Logging
import uk.ac.wellcome.models._
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.transformer.transformers.sierra._
import uk.ac.wellcome.transformer.source.{SierraBibData, SierraItemData}
import uk.ac.wellcome.utils.JsonUtil

import scala.util.{Failure, Success, Try}

class SierraTransformableTransformer
    extends TransformableTransformer[SierraTransformable]
    with SierraPublishers
    with Logging {

  // Populate wwork:title.  The rules are as follows:
  //
  //    For all bibliographic records use Sierra "title".
  //
  // Note: Sierra populates this field from MARC field 245 subfields $a and $b.
  // http://www.loc.gov/marc/bibliographic/bd245.html
  private def getTitle(bibData: SierraBibData): String =
    bibData.title

  private def extractItemData(itemRecord: SierraItemRecord) = {
    info(s"Attempting to transform $itemRecord")

    JsonUtil
      .fromJson[SierraItemData](itemRecord.data) match {
      case Success(sierraItemData) =>
        Some(
          Item(
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
            visible = !sierraItemData.deleted
          ))
      case Failure(e) => {
        error(s"Failed to parse item!", e)

        None
      }
    }
  }

  override def transformForType(
    sierraTransformable: SierraTransformable
  ): Try[Option[Work]] = {
    sierraTransformable.maybeBibData
      .map { bibData =>
        info(s"Attempting to transform $bibData")

        JsonUtil.fromJson[SierraBibData](bibData.data).map { sierraBibData =>
          Some(Work(
            title = getTitle(sierraBibData),
            publishers = getPublishers(sierraBibData),
            sourceIdentifier = SourceIdentifier(
              identifierScheme = IdentifierSchemes.sierraSystemNumber,
              sierraBibData.id
            ),
            identifiers = List(
              SourceIdentifier(
                identifierScheme = IdentifierSchemes.sierraSystemNumber,
                sierraBibData.id
              )
            ),
            items = Option(sierraTransformable.itemData)
              .getOrElse(Map.empty)
              .values
              .flatMap(extractItemData)
              .toList,
            visible = !(sierraBibData.deleted || sierraBibData.suppressed)
          ))
        }

      }
      // A merged record can have both bibs and items.  If we only have
      // the item data so far, we don't have enough to build a Work, so we
      // return None.
      .getOrElse(Success(None))
  }
}
