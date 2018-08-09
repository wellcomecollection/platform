package uk.ac.wellcome.platform.transformer.transformers

import uk.ac.wellcome.models.transformable.{SierraTransformable, Transformable}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.SierraBibData
import uk.ac.wellcome.platform.transformer.transformers.sierra._
import uk.ac.wellcome.json.JsonUtil._

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
    with SierraTitle
    with SierraLocation
    with SierraProduction
    with SierraDimensions
    with SierraSubjects
    with SierraGenres
    with SierraMergeCandidates {

  override def transformForType
    : PartialFunction[(Transformable, Int), Try[TransformedBaseWork]] = {
    case (sierraTransformable: SierraTransformable, version: Int) =>
      val bibId = sierraTransformable.sierraId
      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Work",
        value = bibId.withCheckDigit
      )

      sierraTransformable.maybeBibRecord
        .map { bibRecord =>
          debug(s"Attempting to transform ${bibRecord.id}")

          fromJson[SierraBibData](bibRecord.data)
            .map { sierraBibData =>
              if (!(sierraBibData.deleted || sierraBibData.suppressed)) {
                UnidentifiedWork(
                  sourceIdentifier = sourceIdentifier,
                  otherIdentifiers = getOtherIdentifiers(bibId),
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
                  items =
                    getPhysicalItems(sierraTransformable) ++
                      getDigitalItems(
                        sourceIdentifier.copy(ontologyType = "Item"),
                        sierraBibData),
                  itemsV1 = List(),
                  version = version
                )
              } else {
                throw new ShouldNotTransformException(
                  s"Sierra record ${bibRecord.id} is either deleted or suppressed!"
                )
              }
            }
            .recover {
              case e: ShouldNotTransformException =>
                info(s"Should not transform $bibId: ${e.getMessage}")
                UnidentifiedInvisibleWork(
                  sourceIdentifier = sourceIdentifier,
                  version = version
                )
            }
        }

        // A merged record can have both bibs and items.  If we only have
        // the item data so far, we don't have enough to build a Work to show
        // in the API, so we return an InvisibleWork.
        .getOrElse {
          debug(s"No bib data for ${sierraTransformable.sierraId}, so skipping")
          Success(
            UnidentifiedInvisibleWork(
              sourceIdentifier = sourceIdentifier,
              version = version
            )
          )
        }
  }

}
