package uk.ac.wellcome.platform.transformer.transformers

import com.twitter.inject.Logging
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.SierraBibData
import uk.ac.wellcome.platform.transformer.transformers.sierra._
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.Success

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
    with SierraTitle
    with SierraLocation
    with SierraProduction
    with SierraDimensions
    with SierraSubjects
    with SierraGenres
    with SierraMergeCandidates
    with Logging {

  override def transformForType = {
    case (sierraTransformable: SierraTransformable, version: Int) =>
      sierraTransformable.maybeBibData
        .map { bibData =>
          debug(s"Attempting to transform ${bibData.id}")

          fromJson[SierraBibData](bibData.data).map { sierraBibData =>
            val identifier = SourceIdentifier(
              identifierType = IdentifierType("sierra-system-number"),
              ontologyType = "Work",
              value = addCheckDigit(
                sierraBibData.id,
                recordType = SierraRecordTypes.bibs
              ))
            if (!(sierraBibData.deleted || sierraBibData.suppressed)) {
              Some(
                UnidentifiedWork(
                  sourceIdentifier = identifier,
                  otherIdentifiers = getOtherIdentifiers(sierraBibData),
                  mergeCandidates = getMergeCandidates(sierraBibData),
                  title = getTitle(sierraBibData),
                  workType = getWorkType(sierraBibData),
                  description = getDescription(sierraBibData),
                  physicalDescription = getPhysicalDescription(sierraBibData),
                  extent = getExtent(sierraBibData),
                  lettering = getLettering(sierraBibData),
                  createdDate = None,
                  subjects = getSubjects(sierraBibData),
                  genres = getGenres(sierraBibData),
                  contributors = getContributors(sierraBibData),
                  thumbnail = None,
                  production = getProduction(sierraBibData),
                  language = getLanguage(sierraBibData),
                  dimensions = getDimensions(sierraBibData),
                  items = getItems(sierraTransformable),
                  version = version
                ))
            } else {
              Some(UnidentifiedInvisibleWork(identifier, version))
            }
          }
        }
        // A merged record can have both bibs and items.  If we only have
        // the item data so far, we don't have enough to build a Work, so we
        // return None.
        .getOrElse {
          debug("No bib data on the record, so skipping")
          Success(None)
        }
  }

}
