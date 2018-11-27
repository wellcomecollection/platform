package uk.ac.wellcome.platform.transformer.miro

import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
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
      data = fromJson[MiroTransformableData](transformable.data).get,
      metadata = MiroMetadata(isClearedForCatalogueAPI = true),
      version = version
    )

  def transform(
    data: MiroTransformableData,
    metadata: MiroMetadata,
    version: Int
  ): Try[TransformedBaseWork] =
    doTransform(data, metadata, version) map { transformed =>
      debug(s"Transformed record to $transformed")
      transformed
    } recover {
      case e: Throwable =>
        error("Failed to perform transform to unified item", e)
        throw e
    }

  private def doTransform(
    data: MiroTransformableData,
    metadata: MiroMetadata,
    version: Int): Try[TransformedBaseWork] = {
    val sourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      ontologyType = "Work",
      value = data.miroId
    )

    Try {
      // This is super hacky and we should really remove it (probably fix the
      // source data properly now we have the reporting pipeline), but for
      // now it'll do.
      val miroData = MiroTransformableData.create(toJson(data).get)

      // These images should really have been removed from the pipeline
      // already, but we have at least one instance (B0010525).  It was
      // throwing a MatchError when we tried to pick a license, so handle
      // it properly here.
      if (!miroData.copyrightCleared.contains("Y")) {
        throw new ShouldNotTransformException(
          s"Image ${data.miroId} does not have copyright clearance!"
        )
      }

      val (title, description) = getTitleAndDescription(miroData)

      UnidentifiedWork(
        sourceIdentifier = sourceIdentifier,
        otherIdentifiers =
          getOtherIdentifiers(miroData, data.miroId),
        mergeCandidates = List(),
        title = title,
        workType = getWorkType,
        description = description,
        physicalDescription = None,
        extent = None,
        lettering = miroData.suppLettering,
        createdDate =
          getCreatedDate(miroData, miroId = data.miroId),
        subjects = getSubjects(miroData),
        genres = getGenres(miroData),
        contributors = getContributors(
          miroId = data.miroId,
          miroData = miroData
        ),
        thumbnail = Some(getThumbnail(miroData, data.miroId)),
        production = List(),
        language = None,
        dimensions = None,
        items = getItems(miroData, data.miroId),
        itemsV1 = getItemsV1(miroData, data.miroId),
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
