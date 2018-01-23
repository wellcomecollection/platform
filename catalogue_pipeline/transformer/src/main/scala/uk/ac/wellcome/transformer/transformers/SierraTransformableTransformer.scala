package uk.ac.wellcome.transformer.transformers

import com.twitter.inject.Logging
import uk.ac.wellcome.models._
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.transformer.transformers.sierra._
import uk.ac.wellcome.transformer.source.{SierraBibData, SierraItemData}
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.{Failure, Success, Try}

class SierraTransformableTransformer
    extends TransformableTransformer[SierraTransformable]
    with SierraIdentifiers
    with SierraDescription
    with SierraPublishers
    with SierraTitle
    with Logging {

  private def extractItemData(itemRecord: SierraItemRecord) = {
    info(s"Attempting to transform $itemRecord")

    fromJson[SierraItemData](itemRecord.data) match {
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

        fromJson[SierraBibData](bibData.data).map { sierraBibData =>
          Some(
            Work(
              title = getTitle(sierraBibData),
              sourceIdentifier = SourceIdentifier(
                identifierScheme = IdentifierSchemes.sierraSystemNumber,
                sierraBibData.id
              ),
              identifiers = getIdentifiers(sierraBibData),
              description = getDescription(sierraBibData),
              items = Option(sierraTransformable.itemData)
                .getOrElse(Map.empty)
                .values
                .flatMap(extractItemData)
                .toList,
              publishers = getPublishers(sierraBibData),
              visible = !(sierraBibData.deleted || sierraBibData.suppressed)
            ))
        }

      }
      // A merged record can have both bibs and items.  If we only have
      // the item data so far, we don't have enough to build a Work, so we
      // return None.
      .getOrElse {
        info("No bib data on the record, so skipping")
        Success(None)
      }
  }
}
