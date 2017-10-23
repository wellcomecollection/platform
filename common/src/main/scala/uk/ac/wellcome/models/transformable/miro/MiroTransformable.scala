package uk.ac.wellcome.models.transformable.miro

import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.models._
import uk.ac.wellcome.models.transformable._

import scala.util.Try

case class MiroTransformable(MiroID: String,
                             MiroCollection: String,
                             data: String,
                             ReindexShard: String = "default",
                             ReindexVersion: Int = 0)
    extends Transformable
    with Reindexable[String] {

  val id: ItemIdentifier[String] = ItemIdentifier(
    HashKey("MiroID", MiroID),
    RangeKey("MiroCollection", MiroCollection)
  )

  def collectionIsV(c: String) = c.toLowerCase.contains("images-v")

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

    val description =
      if (rawDescription.trim.length > 0) {
        Some(rawDescription.trim)
      } else None

    (title, description)
  }

  /*
   * <image_creator>: the Creator, which maps to our property "hasCreator"
   */
  def getCreators(miroData: MiroTransformableData): List[Agent] = {
    val primaryCreators = miroData.creator match {
      case Some(c) => c.map { Agent(_) }
      case None => List()
    }

    // <image_secondary_creator>: what MIRO calls Secondary Creator, which
    // will also just have to map to our object property "hasCreator"
    val secondaryCreators = miroData.secondaryCreator match {
      case Some(c) => c.map { Agent(_) }
      case None => List()
    }

    primaryCreators ++ secondaryCreators
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
  def getSubjects(miroData: MiroTransformableData): List[Concept] = {
    val keywords: List[Concept] = miroData.keywords match {
      case Some(k) =>
        k.map { Concept(_) }
      case None => List()
    }

    val keywordsUnauth: List[Concept] = miroData.keywordsUnauth match {
      case Some(k) => k.map { Concept(_) }
      case None => List()
    }

    keywords ++ keywordsUnauth
  }

  def getGenres(miroData: MiroTransformableData): List[Concept] = {
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

  def getCreatedDate(miroData: MiroTransformableData): Option[Period] =
    if (collectionIsV(MiroCollection)) {
      miroData.artworkDate.map { Period(_) }
    } else {
      None
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

  def getThumbnail(miroData: MiroTransformableData): Location = {
    Location(
      locationType = "thumbnail-image",
      url = Some(buildImageApiURL(MiroID, "thumbnail")),
      license = chooseLicense(miroData.useRestrictions.get)
    )
  }

  def getItems(miroData: MiroTransformableData): List[Item] = {
    List(
      Item(
        identifiers = List(
          SourceIdentifier(IdentifierSchemes.miroImageNumber, MiroID)
        ),
        locations = List(
          Location(
            locationType = "iiif-image",
            url = Some(buildImageApiURL(MiroID, "info")),
            license = chooseLicense(miroData.useRestrictions.get)
          )
        )
      ))
  }

  def getIdentifiers(miroData: MiroTransformableData): List[SourceIdentifier] = {
    val miroIDList = List(
      SourceIdentifier(IdentifierSchemes.miroImageNumber, MiroID)
    )

    // Add the Sierra system number from the INNOPAC ID, if it's present.
    //
    // We add a b-prefix because everything in Miro is a bibliographic record,
    // but there are other types in Sierra (e.g. item, holding) with matching
    // IDs but different prefixes.
    val sierraList: List[SourceIdentifier] = miroData.innopacID match {
      case Some(s) => {

        // The ID in the Miro record is an 8-digit number with a check digit
        // (which may be x), but the system number is 7-digits, sans checksum.
        val regexMatch = """^([0-9]{7})[0-9xX]$""".r.unapplySeq(s)
        regexMatch match {
          case Some(s) =>
            s.map { id =>
              SourceIdentifier(IdentifierSchemes.sierraSystemNumber, s"b$id")
            }
          case _ =>
            throw new RuntimeException(
              s"Expected 8-digit INNOPAC ID or nothing, got ${miroData.innopacID}"
            )
        }
      }
      case None => List()
    }

    miroIDList ++ sierraList
  }

  override def transform: Try[Work] = Try {

    val miroData = MiroTransformableData.create(data)
    val (title, description) = getTitleAndDescription(miroData)

    Work(
      identifiers = getIdentifiers(miroData),
      title = title,
      description = description,
      createdDate = getCreatedDate(miroData),
      lettering = miroData.suppLettering,
      creators = getCreators(miroData),
      subjects = getSubjects(miroData),
      genres = getGenres(miroData),
      thumbnail = Some(getThumbnail(miroData)),
      items = getItems(miroData)
    )
  }
}
