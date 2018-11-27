package uk.ac.wellcome.platform.transformer.miro

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.exceptions.ShouldNotTransformException
import uk.ac.wellcome.platform.transformer.miro.models.{MiroMetadata, MiroTransformable}
import uk.ac.wellcome.platform.transformer.miro.source.MiroTransformableData

import scala.util.Try

class MiroTransformableTransformer
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

  def transform(
    transformable: MiroTransformable,
    version: Int
  ): Try[TransformedBaseWork] =
    transform(
      transformable = transformable,
      metadata = MiroMetadata(isClearedForCatalogueAPI = true),
      version = version
    )

  def transform(
    transformable: MiroTransformable,
    metadata: MiroMetadata,
    version: Int
  ): Try[TransformedBaseWork] =
    doTransform(transformable, metadata, version) map { transformed =>
      debug(s"Transformed record to $transformed")
      transformed
    } recover {
      case e: Throwable =>
        error("Failed to perform transform to unified item", e)
        throw e
    }

  private def doTransform(
    miroTransformable: MiroTransformable,
    metadata: MiroMetadata,
    version: Int): Try[TransformedBaseWork] = {
    val sourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      ontologyType = "Work",
      value = miroTransformable.sourceId
    )

    Try {
      val miroData = MiroTransformableData.create(miroTransformable.data)

      // These images should really have been removed from the pipeline
      // already, but we have at least one instance (B0010525).  It was
      // throwing a MatchError when we tried to pick a license, so handle
      // it properly here.
      if (!miroData.copyrightCleared.contains("Y")) {
        throw new ShouldNotTransformException(
          s"Image ${miroTransformable.sourceId} does not have copyright clearance!"
        )
      }

      val (title, description) = getTitleAndDescription(miroData)

      UnidentifiedWork(
        sourceIdentifier = sourceIdentifier,
        otherIdentifiers =
          getOtherIdentifiers(miroData, miroTransformable.sourceId),
        mergeCandidates = List(),
        title = title,
        workType = getWorkType,
        description = description,
        physicalDescription = None,
        extent = None,
        lettering = miroData.suppLettering,
        createdDate =
          getCreatedDate(miroData, miroId = miroTransformable.sourceId),
        subjects = getSubjects(miroData),
        genres = getGenres(miroData),
        contributors = getContributors(
          miroId = miroTransformable.sourceId,
          miroData = miroData
        ),
        thumbnail = Some(getThumbnail(miroData, miroTransformable.sourceId)),
        production = List(),
        language = None,
        dimensions = None,
        items = getItems(miroData, miroTransformable.sourceId),
        itemsV1 = getItemsV1(miroData, miroTransformable.sourceId),
        version = version
      )
    }.recover {
      case e: ShouldNotTransformException =>
        info(s"Should not transform: ${e.getMessage}")
        UnidentifiedInvisibleWork(
          sourceIdentifier = sourceIdentifier,
          version = version
        )
    }
  }
}
