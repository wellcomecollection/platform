package uk.ac.wellcome.platform.transformer.sierra

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.exceptions.JsonDecodingError
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.{
  SierraItemNumber,
  SierraItemRecord
}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.exceptions.{
  ShouldNotTransformException,
  SierraTransformerException
}
import uk.ac.wellcome.platform.transformer.sierra.source.{
  SierraBibData,
  SierraItemData
}
import uk.ac.wellcome.platform.transformer.sierra.transformers._
import uk.ac.wellcome.platform.transformer.sierra.source.SierraMaterialType._

import scala.util.{Failure, Success, Try}

class SierraTransformableTransformer
    extends SierraIdentifiers
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
    with SierraGenres
    with SierraMergeCandidates
    with SierraSubjects {

  def transform(
    transformable: SierraTransformable,
    version: Int
  ): Try[TransformedBaseWork] = {
    doTransform(transformable, version) map { transformed =>
      debug(s"Transformed record to $transformed")
      transformed
    } recover {
      case e: Throwable =>
        error("Failed to perform transform to unified item", e)
        throw e
    }
  }

  private def doTransform(sierraTransformable: SierraTransformable,
                          version: Int): Try[TransformedBaseWork] = {
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
            val sierraItemDataMap =
              extractItemData(sierraTransformable.itemRecords)

            if (!(sierraBibData.deleted || sierraBibData.suppressed)) {
              UnidentifiedWork(
                sourceIdentifier = sourceIdentifier,
                otherIdentifiers = getOtherIdentifiers(
                  bibId = bibId,
                  bibData = sierraBibData
                ),
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
                items = getItems(
                  bibId = bibId,
                  bibData = sierraBibData,
                  itemDataMap = sierraItemDataMap
                ),
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
            case e: JsonDecodingError =>
              throw SierraTransformerException(
                s"Unable to parse bib data for ${bibRecord.id} as JSON: <<${bibRecord.data}>>"
              )
            case e: ShouldNotTransformException =>
              debug(s"Should not transform $bibId: ${e.getMessage}")
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

  def extractItemData(itemRecords: Map[SierraItemNumber, SierraItemRecord])
    : Map[SierraItemNumber, SierraItemData] =
    itemRecords
      .map { case (id, itemRecord) => (id, itemRecord.data) }
      .map {
        case (id, jsonString) =>
          fromJson[SierraItemData](jsonString) match {
            case Success(data) => id -> data
            case Failure(_) =>
              throw SierraTransformerException(
                s"Unable to parse item data for $id as JSON: <<$jsonString>>")
          }
      }

}
