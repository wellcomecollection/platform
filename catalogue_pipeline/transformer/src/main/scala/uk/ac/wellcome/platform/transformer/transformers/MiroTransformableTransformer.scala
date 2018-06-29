package uk.ac.wellcome.platform.transformer.transformers

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.MiroTransformableData
import uk.ac.wellcome.platform.transformer.transformers.miro._

import scala.util.Try

class MiroTransformableTransformer
    extends TransformableTransformer[MiroTransformable]
    with MiroContributors
    with MiroCredits
    with MiroGenres
    with MiroIdentifiers
    with MiroLicenses
    with MiroSubjects
    with MiroTransformableUtils
    with Logging {
  // TODO this class is too big as the different test classes would suggest. Split it.

  override def transformForType(miroTransformable: MiroTransformable,
                                version: Int): Try[Option[UnidentifiedWork]] =
    Try {
      val miroData = MiroTransformableData.create(miroTransformable.data)
      val (title, description) = getTitleAndDescription(miroData)

      try {

        // We had a request to remove records from contributor GUS from
        // the API after we'd done the initial sort of the Miro data.
        // We removed them from Elasticsearch by hand, but we don't want
        // them to reappear on reindex.
        //
        // Until we can move them to Tandem Vault or Cold Store, we'll throw
        // them away at this point.
        if (miroData.sourceCode == Some("GUS")) {
          throw new ShouldNotTransformException(
            "Images from contributor GUS should not be sent to the pipeline"
          )
        }

        // These images should really have been removed from the pipeline
        // already, but we have at least one instance (B0010525).  It was
        // throwing a MatchError when we tried to pick a license, so handle
        // it properly here.
        if (miroData.copyrightCleared != Some("Y")) {
          throw new ShouldNotTransformException(
            s"Image ${miroTransformable.sourceId} does not have copyright clearance!"
          )
        }

        Some(
          UnidentifiedWork(
            title = Some(title),
            sourceIdentifier = SourceIdentifier(
              identifierType = IdentifierType("miro-image-number"),
              ontologyType = "Work",
              value = miroTransformable.sourceId),
            version = version,
            otherIdentifiers =
              getOtherIdentifiers(miroData, miroTransformable.sourceId),
            description = description,
            lettering = miroData.suppLettering,
            createdDate =
              getCreatedDate(miroData, miroTransformable.MiroCollection),
            subjects = getSubjects(miroData),
            contributors = getContributors(
              miroId = miroTransformable.sourceId,
              miroData = miroData
            ),
            genres = getGenres(miroData),
            thumbnail = Some(getThumbnail(miroData, miroTransformable.sourceId)),
            items = getItems(miroData, miroTransformable.sourceId)
          ))
      } catch {
        case e: ShouldNotTransformException => {
          warn(
            s"Should not transform ${miroTransformable.sourceId}: ${e.getMessage}")
          None
        }
      }
    }

  /*
   * Populate the title and description.  The rules are as follows:
   *
   * 1.  For V images, if the first line of <image_image_desc> is a
   *     prefix of <image_title>, we use that instead of the title, and
   *     drop the first line of the description.
   *     If it's a single-line description, drop the description entirely.
   * 2.  Otherwise, use the <image_title> ("short description") and
   *     <image_image_desc> ("description") fields.
   *
   * In at least the V collection, many of the titles are truncated forms
   * of the description field -- and we don't want to repeat information
   * in the public API.
   *
   * Note: Every image in the V collection that has image_cleared == Y has
   * non-empty title.  This is _not_ true for the MIRO records in general.
   *
   * TODO: Work out what title to use for those records.
   *
   *  In Miro, the <image_image_desc> and <image_image_title> fields are
   *  filled in by the cataloguer at point of import.  There's also an
   *  <image_image_desc_academic> field which contains a description
   *  taken from Sierra.  In some cases, the cataloguer hasn't copied any
   *  of this field over the desc/title, just leaving them as "--"/"-".
   *
   *  Since desc_academic is exposed publicly via Sierra, we can use it
   *  here if there's nothing more useful in the other fields.
   */
  private def getTitleAndDescription(
    miroData: MiroTransformableData): (String, Option[String]) = {

    val candidateDescription: String = miroData.description match {
      case Some(s) => {
        if (s == "--" || s == "-") miroData.academicDescription.getOrElse("")
        else s
      }
      case None => ""
    }

    val candidateTitle = candidateDescription.split("\n").head
    val titleIsTruncatedDescription = miroData.title match {
      case Some(title) => candidateTitle.startsWith(title)
      case None        => true
    }

    val useDescriptionAsTitle =
      (titleIsTruncatedDescription) ||
        (miroData.title.get == "-" || miroData.title.get == "--")

    val title = if (useDescriptionAsTitle) {
      candidateTitle
    } else miroData.title.get

    val rawDescription = if (useDescriptionAsTitle) {
      // Remove the first line from the description, and trim any extra
      // whitespace (leading newlines)
      candidateDescription.replace(candidateTitle, "")
    } else {
      candidateDescription
    }

    // Add any information about Wellcome Image Awards winners to the
    // description.  We append a sentence to the description, using one
    // of the following patterns:
    //
    //    Biomedical Image Awards 1997.
    //    Wellcome Image Awards 2015.
    //    Wellcome Image Awards 2016 Overall Winner.
    //    Wellcome Image Awards 2017, Julie Dorrington Award Winner.
    //
    // For now, any other award data gets discarded.
    val wiaAwardsData: List[(String, String)] =
      zipMiroFields(keys = miroData.award, values = miroData.awardDate)
        .collect {
          case (Some(label), Some(year))
              if label == "WIA Overall Winner" ||
                label == "Wellcome Image Awards" ||
                label == "Biomedical Image Awards" =>
            (label, year)
        }

    val wiaAwardsString = wiaAwardsData match {
      // Most images have no award, or only a single award string.
      case Nil                 => ""
      case List((label, year)) => s" $label $year."

      // A handful of images have an award key pair for "WIA Overall Winner"
      // and "Wellcome Image Awards", both with the same year.  In this case,
      // we write a single sentence.
      case List((_, year), (_, _)) =>
        s" Wellcome Image Awards Overall Winner $year."

      // Any more than two award-related entries in these fields would be
      // unexpected, and we let it error as an unmatched case.
    }

    // Finally, remove any leading/trailing from the description, and drop
    // the description if it's *only* whitespace.
    val description =
      if (!(rawDescription + wiaAwardsString).trim.isEmpty) {
        Some((rawDescription + wiaAwardsString).trim)
      } else None

    (title, description)
  }

  private def getThumbnail(miroData: MiroTransformableData,
                           miroId: String): Location = {
    DigitalLocation(
      locationType = LocationType("thumbnail-image"),
      url = buildImageApiURL(miroId, "thumbnail"),
      license = chooseLicense(miroData.useRestrictions)
    )
  }

  private def getItems(miroData: MiroTransformableData,
                       miroId: String): List[Identifiable[Item]] = {
    List(
      Identifiable(
        sourceIdentifier = SourceIdentifier(
          identifierType = IdentifierType("miro-image-number"),
          "Item",
          miroId),
        agent = Item(
          locations = List(
            DigitalLocation(
              locationType = LocationType("iiif-image"),
              url = buildImageApiURL(miroId, "info"),
              credit = getCredit(miroId = miroId, miroData = miroData),
              license = chooseLicense(miroData.useRestrictions)
            )
          )
        )
      ))
  }

  private def getCreatedDate(miroData: MiroTransformableData,
                             collection: String): Option[Period] =
    if (collectionIsV(collection)) {
      miroData.artworkDate.map { Period(_) }
    } else {
      None
    }

  private def collectionIsV(c: String) = c.toLowerCase.contains("images-v")

  private def buildImageApiURL(miroID: String, templateName: String): String = {
    val iiifImageApiBaseUri = "https://iiif.wellcomecollection.org"

    val imageUriTemplates = Map(
      "thumbnail" -> "%s/image/%s.jpg/full/300,/0/default.jpg",
      "info" -> "%s/image/%s.jpg/info.json"
    )

    val imageUriTemplate = imageUriTemplates.getOrElse(
      templateName,
      throw new Exception(
        s"Unrecognised Image API URI template ($templateName)!"))

    imageUriTemplate.format(iiifImageApiBaseUri, miroID)
  }
}
