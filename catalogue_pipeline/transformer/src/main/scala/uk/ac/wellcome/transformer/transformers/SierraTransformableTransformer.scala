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
    with SierraPhysicalDescription
    with SierraExtent
    with SierraItems
    with SierraLettering
    with SierraPublishers
    with SierraTitle
    with SierraLocation
    with SierraPublicationDate
    with SierraPlaceOfPublication
    with Logging {

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
              physicalDescription = getPhysicalDescription(sierraBibData),
              extent = getExtent(sierraBibData),
              lettering = getLettering(sierraBibData),
              items = getItems(sierraTransformable),
              publishers = getPublishers(sierraBibData),
              visible = !(sierraBibData.deleted || sierraBibData.suppressed),
              publicationDate = getPublicationDate(sierraBibData),
              placesOfPublication = getPlacesOfPublication(sierraBibData)
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
