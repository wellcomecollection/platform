package uk.ac.wellcome.transformer.transformers
import java.io.InputStream

import uk.ac.wellcome.models._
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.transformer.source.MiroTransformableData
import uk.ac.wellcome.transformer.transformers.miro.MiroContributors
import uk.ac.wellcome.utils.JsonUtil._

import scala.io.Source
import scala.util.Try

class MiroTransformableTransformer
    extends TransformableTransformer[MiroTransformable]
    with MiroContributors {
  // TODO this class is too big as the different test classes would suggest. Split it.

  override def transformForType(miroTransformable: MiroTransformable,
                                version: Int): Try[Option[UnidentifiedWork]] =
    Try {

      val miroData = MiroTransformableData.create(miroTransformable.data)
      val (title, description) = getTitleAndDescription(miroData)

      Some(
        UnidentifiedWork(
          title = Some(title),
          sourceIdentifier = SourceIdentifier(
            identifierScheme = IdentifierSchemes.miroImageNumber,
            ontologyType = "Work",
            value = miroTransformable.sourceId),
          version = version,
          identifiers = getIdentifiers(miroData, miroTransformable.sourceId),
          description = description,
          lettering = miroData.suppLettering,
          createdDate =
            getCreatedDate(miroData, miroTransformable.MiroCollection),
          subjects = getSubjects(miroData),
          contributors = getContributors(miroData),
          genres = getGenres(miroData),
          thumbnail = Some(getThumbnail(miroData, miroTransformable.sourceId)),
          items = getItems(miroData, miroTransformable.sourceId)
        ))
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
      case None => true
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
      case Nil => ""
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

  private def getIdentifiers(miroData: MiroTransformableData,
                             miroId: String): List[SourceIdentifier] = {
    val miroIDList = List(
      SourceIdentifier(
        identifierScheme = IdentifierSchemes.miroImageNumber,
        ontologyType = "Work",
        value = miroId)
    )

    // Add the Sierra system number from the INNOPAC ID, if it's present.
    //
    // We add a b-prefix because everything in Miro is a bibliographic record,
    // but there are other types in Sierra (e.g. item, holding) with matching
    // IDs but different prefixes.
    val sierraList: List[SourceIdentifier] = miroData.innopacID match {
      case Some(s) => {

        // The ID in the Miro record is an 8-digit number with a check digit
        // (which may be x).  The format we use for Sierra system numbers
        // is a record type prefix ("b") and 8-digits.
        //
        // Regex explanation:
        //
        //    ^                 start of string
        //    (?:\.?[bB])?      non-capturing group, which trims 'b' or 'B'
        //                      or '.b' or '.B' from the start of the string,
        //                      but *not* a lone '.'
        //    ([0-9]{7}[0-9xX]) capturing group, the 8 digits of the system
        //                      number plus the final check digit, which may
        //                      be an X
        //    $                 end of the string
        //
        val regexMatch = """^(?:\.?[bB])?([0-9]{7}[0-9xX])$""".r.unapplySeq(s)
        regexMatch match {
          case Some(s) =>
            s.map { id =>
              SourceIdentifier(
                identifierScheme = IdentifierSchemes.sierraSystemNumber,
                ontologyType = "Work",
                value = s"b$id")
            }
          case _ =>
            throw new RuntimeException(
              s"Expected 8-digit INNOPAC ID or nothing, got ${miroData.innopacID}"
            )
        }
      }
      case None => List()
    }

    // Add any other legacy identifiers to this record.  Right now we
    // put them all in the same identifier scheme, because we're not doing
    // any transformation or cleaning.
    val libraryRefsList: List[SourceIdentifier] =
      zipMiroFields(
        keys = miroData.libraryRefDepartment,
        values = miroData.libraryRefId)
        .collect {
          case (Some(label), Some(value)) =>
            SourceIdentifier(
              identifierScheme = IdentifierSchemes.miroLibraryReference,
              ontologyType = "Work",
              value = s"$label $value"
            )
        }

    miroIDList ++ sierraList ++ libraryRefsList
  }

  /* Populate the subjects field.  This is based on two fields in the XML,
   *  <image_keywords> and <image_keywords_unauth>.  Both of these were
   *  defined in part or whole by the human cataloguers, and in general do
   *  not correspond to a controlled vocabulary.  (The latter was imported
   *  directly from PhotoSoft.)
   *
   *  In some cases, these actually do correspond to controlled vocabs,
   *  e.g. where keywords were pulled directly from Sierra -- but we don't
   *  have enough information in Miro to determine which ones those are.
   */
  private def getSubjects(miroData: MiroTransformableData): List[Subject] = {
    val keywords: List[Subject] = miroData.keywords match {
      case Some(k) =>
        k.map { keyword => Subject(label = keyword, concepts = List(Concept(keyword)) ) }
      case None => List()
    }

    val keywordsUnauth: List[Subject] = miroData.keywordsUnauth match {
      case Some(k) =>
        k.map { keyword => Subject(label = keyword, concepts = List(Concept(keyword)) ) }
      case None => List()
    }

    keywords ++ keywordsUnauth
  }

  private def getGenres(miroData: MiroTransformableData): List[Concept] = {
    // Populate the subjects field.  This is based on two fields in the XML,
    // <image_phys_format> and <image_lc_genre>.
    val physFormat: List[Concept] = miroData.physFormat match {
      case Some(f) => List(Concept(f))
      case None => List()
    }

    val lcGenre: List[Concept] = miroData.lcGenre match {
      case Some(g) => List(Concept(g))
      case None => List()
    }

    (physFormat ++ lcGenre).distinct
  }

  private def getThumbnail(miroData: MiroTransformableData,
                           miroId: String): Location = {
    DigitalLocation(
      locationType = "thumbnail-image",
      url = buildImageApiURL(miroId, "thumbnail"),
      license = chooseLicense(miroData.useRestrictions.get)
    )
  }

  private def getItems(miroData: MiroTransformableData,
                       miroId: String): List[UnidentifiedItem] = {
    List(
      UnidentifiedItem(
        sourceIdentifier =
          SourceIdentifier(IdentifierSchemes.miroImageNumber, "Item", miroId),
        identifiers = List(
          SourceIdentifier(IdentifierSchemes.miroImageNumber, "Item", miroId)
        ),
        locations = List(
          DigitalLocation(
            locationType = "iiif-image",
            url = buildImageApiURL(miroId, "info"),
            credit = getCredit(miroData),
            license = chooseLicense(miroData.useRestrictions.get)
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

  /** Image credits in MIRO could be set in two ways:
    *
    *    - using the image_credit_line, which is per-image
    *    - using the image_source_code, which falls back to a contributor-level
    *      credit line
    *
    * We prefer the per-image credit line, but use the contributor-level credit
    * if unavailable.
    */
  private def getCredit(miroData: MiroTransformableData): Option[String] = {
    miroData.creditLine match {

      // Some of the credit lines are inconsistent or use old names for
      // Wellcome, so we do a bunch of replacements and trimming to tidy
      // them up.
      case Some(line) =>
        Some(line
          .replaceAll(
            "Adrian Wressell, Heart of England NHSFT",
            "Adrian Wressell, Heart of England NHS FT")
          .replaceAll(
            "Andrew Dilley,Jane Greening & Bruce Lynn",
            "Andrew Dilley, Jane Greening & Bruce Lynn")
          .replaceAll(
            "Andrew Dilley,Nicola DeLeon & Bruce Lynn",
            "Andrew Dilley, Nicola De Leon & Bruce Lynn")
          .replaceAll(
            "Ashley Prytherch, Royal Surrey County Hospital NHS Foundation Trust",
            "Ashley Prytherch, Royal Surrey County Hospital NHS FT")
          .replaceAll(
            "David Gregory & Debbie Marshall",
            "David Gregory and Debbie Marshall")
          .replaceAll(
            "David Gregory&Debbie Marshall",
            "David Gregory and Debbie Marshall")
          .replaceAll("Geraldine Thompson.", "Geraldine Thompson")
          .replaceAll("John & Penny Hubley.", "John & Penny Hubley")
          .replaceAll(
            "oyal Army Medical Corps Muniment Collection, Wellcome Images",
            "Royal Army Medical Corps Muniment Collection, Wellcome Collection")
          .replaceAll("Science Museum London", "Science Museum, London")
          .replaceAll("The Wellcome Library, London", "Wellcome Collection")
          .replaceAll("Wellcome Library, London", "Wellcome Collection")
          .replaceAll("Wellcome Libary, London", "Wellcome Collection")
          .replaceAll("Wellcome LIbrary, London", "Wellcome Collection")
          .replaceAll("Wellcome Images", "Wellcome Collection")
          .replaceAll("The Wellcome Library", "Wellcome Collection")
          .replaceAll("Wellcome Library", "Wellcome Collection")
          .replaceAll("Wellcome Collection London", "Wellcome Collection")
          .replaceAll("Wellcome Collection, Londn", "Wellcome Collection")
          .replaceAll("Wellcome Trust", "Wellcome Collection")
          .replaceAll("'Wellcome Collection'", "Wellcome Collection"))

      // Otherwise we carry through the contributor codes, which have
      // already been edited for consistency.
      case None =>
        miroData.sourceCode match {
          case Some(code) => Some(contributorMap(code.toUpperCase))
          case None => None
        }
    }
  }

  /** Some Miro fields contain keys/values in different fields.  For example:
    *
    *     "image_library_ref_department": ["ICV No", "External Reference"],
    *     "image_library_ref_id": ["1234", "Sanskrit manuscript 5678"]
    *
    * This represents the mapping:
    *
    *     "ICV No"             -> "1234"
    *     "External Reference" -> "Sanskrit manuscript 5678"
    *
    * This method takes two such fields, combines them, and returns a list
    * of (key, value) tuples.  Note: we don't use a map because keys aren't
    * guaranteed to be unique.
    */
  def zipMiroFields(
    keys: List[Option[String]],
    values: List[Option[String]]): List[(Option[String], Option[String])] = {

    if (keys.lengthCompare(values.length) != 0) {
      throw new RuntimeException(
        s"Different lengths! keys=$keys, values=$values"
      )
    }

    (keys, values).zipped.toList
  }

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

  /** If the image has a non-empty image_use_restrictions field, choose which
    *  license (if any) we're going to assign to the thumbnail for this work.
    *
    *  The mappings in this function are based on a document provided by
    *  Christy Henshaw (MIRO drop-downs.docx).  There are still some gaps in
    *  that, we'll have to come back and update this code later.
    *
    *  For now, this mapping only covers use restrictions seen in the
    *  V collection.  We'll need to extend this for other licenses later.
    *
    *  TODO: Expand this mapping to cover all of MIRO.
    *  TODO: Update these mappings based on the final version of Christy's
    *        document.
    */
  private def chooseLicense(useRestrictions: String): License =
    useRestrictions match {

      // Certain strings map directly onto license types
      case "CC-0" => License_CC0
      case "CC-BY" => License_CCBY
      case "CC-BY-NC" => License_CCBYNC
      case "CC-BY-NC-ND" => License_CCBYNCND

      // These mappings are defined in Christy's document
      case "Academics" => License_CCBYNC
    }

}
