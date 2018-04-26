package uk.ac.wellcome.transformer.transformers

import com.twitter.inject.Logging
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.transformer.source.SierraBibData
import uk.ac.wellcome.transformer.transformers.sierra._
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.work_model.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork}

import scala.util.{Success, Try}

class SierraTransformableTransformer
    extends TransformableTransformer[SierraTransformable]
    with SierraIdentifiers
    with SierraContributors
    with SierraDescription
    with SierraPhysicalDescription
    with SierraWorkType
    with SierraExtent
    with SierraItems
    with SierraLanguage
    with SierraLettering
    with SierraPublishers
    with SierraTitle
    with SierraLocation
    with SierraPublicationDate
    with SierraPlaceOfPublication
    with SierraDimensions
    with SierraSubjects
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
                ontologyType = "Work",
                value = addCheckDigit(
                  sierraBibData.id,
                  recordType = SierraRecordTypes.bibs
                )
              ),
              version = version,
              identifiers = getIdentifiers(sierraBibData),
              workType = getWorkType(sierraBibData),
              description = getDescription(sierraBibData),
              physicalDescription = getPhysicalDescription(sierraBibData),
              extent = getExtent(sierraBibData),
              lettering = getLettering(sierraBibData),
              items = getItems(sierraTransformable),
              publishers = getPublishers(sierraBibData),
              visible = !(sierraBibData.deleted || sierraBibData.suppressed),
              publicationDate = getPublicationDate(sierraBibData),
              placesOfPublication = getPlacesOfPublication(sierraBibData),
              language = getLanguage(sierraBibData),
              contributors = getContributors(sierraBibData),
              dimensions = getDimensions(sierraBibData),
              subjects = getSubjects(sierraBibData)
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
