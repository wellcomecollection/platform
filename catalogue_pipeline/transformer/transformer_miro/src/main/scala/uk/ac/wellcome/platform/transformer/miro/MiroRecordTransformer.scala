package uk.ac.wellcome.platform.transformer.miro

import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.miro.exceptions.ShouldNotTransformException
import uk.ac.wellcome.platform.transformer.miro.models.MiroMetadata
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

import scala.util.Try

class MiroRecordTransformer
    extends transformers.MiroContributors
    with transformers.MiroCreatedDate
    with transformers.MiroItems
    with transformers.MiroGenres
    with transformers.MiroIdentifiers
    with transformers.MiroSubjects
    with transformers.MiroThumbnail
    with transformers.MiroTitleAndDescription
    with transformers.MiroWorkType
    with Logging {

  def transform(miroRecord: MiroRecord,
                miroMetadata: MiroMetadata,
                version: Int): Try[TransformedBaseWork] =
    doTransform(miroRecord, miroMetadata, version) map { transformed =>
      debug(s"Transformed record to $transformed")
      transformed
    } recover {
      case e: Throwable =>
        error("Failed to perform transform to unified item", e)
        throw e
    }

  private def doTransform(originalMiroRecord: MiroRecord,
                          miroMetadata: MiroMetadata,
                          version: Int): Try[TransformedBaseWork] = {
    val sourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      ontologyType = "Work",
      value = originalMiroRecord.imageNumber
    )

    Try {
      // Any records that aren't cleared for the Catalogue API should be
      // discarded immediately.
      if (!miroMetadata.isClearedForCatalogueAPI) {
        throw new ShouldNotTransformException(
          s"Image ${originalMiroRecord.imageNumber} is not cleared for the API!"
        )
      }

      // This is an utterly awful hack we have to live with until we get
      // these corrected in the source data.
      val miroRecord = MiroRecord.create(toJson(originalMiroRecord).get)

      // These images should really have been removed from the pipeline
      // already, but we have at least one instance (B0010525).  It was
      // throwing a MatchError when we tried to pick a license, so handle
      // it properly here.
      if (!miroRecord.copyrightCleared.contains("Y")) {
        throw new ShouldNotTransformException(
          s"Image ${miroRecord.imageNumber} does not have copyright clearance!"
        )
      }

      val (title, description) = getTitleAndDescription(miroRecord)

      UnidentifiedWork(
        sourceIdentifier = sourceIdentifier,
        otherIdentifiers = getOtherIdentifiers(miroRecord),
        mergeCandidates = List(),
        title = title,
        workType = getWorkType,
        description = description,
        physicalDescription = None,
        extent = None,
        lettering = miroRecord.suppLettering,
        createdDate = getCreatedDate(miroRecord),
        subjects = getSubjects(miroRecord),
        genres = getGenres(miroRecord),
        contributors = getContributors(miroRecord),
        thumbnail = Some(getThumbnail(miroRecord)),
        production = List(),
        language = None,
        dimensions = None,
        items = getItems(miroRecord),
        itemsV1 = getItemsV1(miroRecord),
        version = version
      )
    }.recover {
      case e: ShouldNotTransformException =>
        debug(s"Should not transform: ${e.getMessage}")
        UnidentifiedInvisibleWork(
          sourceIdentifier = sourceIdentifier,
          version = version
        )
    }
  }
}
