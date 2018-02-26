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
    with SierraLettering
    with SierraPublishers
    with SierraTitle
    with SierraLocation
    with SierraPublicationDate
    with Logging {

  private def extractItemData(itemRecord: SierraItemRecord) = {
    info(s"Attempting to transform ${itemRecord.id}")

    fromJson[SierraItemData](itemRecord.data) match {
      case Success(sierraItemData) =>
        Some(
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
          ))
      case Failure(e) => {
        error(s"Failed to parse item!", e)
        None
      }
    }
  }

  override def transformForType(
    sierraTransformable: SierraTransformable,
    version: Int
  ): Try[Option[UnidentifiedWork]] = {
    sierraTransformable.maybeBibData
      .map { bibData =>
        info(s"Attempting to transform ${bibData.id}")

        fromJson[SierraBibData](bibData.data).map { sierraBibData =>
          Some(
            UnidentifiedWork(
              title = getTitle(sierraBibData),
              sourceIdentifier = SourceIdentifier(
                identifierScheme = IdentifierSchemes.sierraSystemNumber,
                sierraBibData.id
              ),
              version = version,
              identifiers = getIdentifiers(sierraBibData),
              description = getDescription(sierraBibData),
              lettering = getLettering(sierraBibData),
              items = Option(sierraTransformable.itemData)
                .getOrElse(Map.empty)
                .values
                .flatMap(extractItemData)
                .toList,
              publishers = getPublishers(sierraBibData),
              visible = !(sierraBibData.deleted || sierraBibData.suppressed),
              publicationDate = getPublicationDate(sierraBibData)
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
