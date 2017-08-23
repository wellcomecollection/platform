package uk.ac.wellcome.models

import scala.util.Try

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang.StringEscapeUtils

import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.utils.JsonUtil

case class MiroTransformableData(
  @JsonProperty("image_title") title: Option[String],
  @JsonProperty("image_creator") creator: Option[List[String]],
  @JsonProperty("image_image_desc") description: Option[String],
  @JsonProperty("image_image_desc_academic") academicDescription: Option[
    String],
  @JsonProperty("image_secondary_creator") secondaryCreator: Option[
    List[String]],
  @JsonProperty("image_artwork_date") artworkDate: Option[String],
  @JsonProperty("image_cleared") cleared: Option[String],
  @JsonProperty("image_copyright_cleared") copyrightCleared: Option[String],
  @JsonProperty("image_keywords") keywords: Option[List[String]],
  @JsonProperty("image_keywords_unauth") keywordsUnauth: Option[List[String]],
  @JsonProperty("image_phys_format") physFormat: Option[String],
  @JsonProperty("image_lc_genre") lcGenre: Option[String],
  @JsonProperty("image_tech_file_size") techFileSize: Option[List[String]],
  @JsonProperty("image_use_restrictions") useRestrictions: Option[String]
)

case class ShouldNotTransformException(field: String,
                                       value: Any,
                                       message: String = "")
    extends Exception(
      if (message.isEmpty)
        s"$field='${value.toString}' ($message)"
      else
        s"$field='${value.toString}'"
    )

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

  override def transform: Try[Work] = {

    // Some of the Miro fields were imported from Sierra, and had special
    // characters replaced by HTML-encoded entities when copied across.
    // We need to fix them up before we decode as JSON.
    val unencodedData = StringEscapeUtils.unescapeHtml(data)

    JsonUtil.fromJson[MiroTransformableData](unencodedData).map { miroData =>
      // Identifier is passed straight through
      val identifiers =
        List(SourceIdentifier(IdentifierSchemes.miroImageNumber, MiroID))

      // XML tags refer to fields within the Miro XML dumps.

      // If the <image_cleared> or <image_copyright_cleared> fields are
      // missing or don't have have the value 'Y', then we shouldn't expose
      // this image in the public API.
      // See https://github.com/wellcometrust/platform-api/issues/356
      if (miroData.cleared.getOrElse("N") != "Y") {
        throw new ShouldNotTransformException(
          field = "image_cleared",
          value = miroData.cleared
        )
      }
      if (miroData.copyrightCleared.getOrElse("N") != "Y") {
        throw new ShouldNotTransformException(
          field = "image_copyright_cleared",
          value = miroData.copyrightCleared
        )
      }

      // There are a bunch of <image_tech_*> fields that refer to the
      // underlying image file.  If these are empty, there isn't actually a
      // file to retrieve, which breaks the Collection site.  Sometimes this is
      // a "glue" record that refers to multiple images.  e.g. V0011212ETL
      //
      // Eventually it might be nice to collate these -- have all the images
      // in the same API result, but for now, we just exclude them from
      // the API.  They aren't useful for testing image search.
      if (miroData.techFileSize.getOrElse(List[String]()).isEmpty) {
        throw new ShouldNotTransformException(
          field = "image_tech_file_size",
          value = miroData.techFileSize,
          message =
            "Missing image_tech_file_size means there is no underlying image"
        )
      }

      // In Miro, the <image_image_desc> and <image_image_title> fields are
      // filled in by the cataloguer at point of import.  There's also an
      // <image_image_desc_academic> field which contains a description
      // taken from Sierra.  In some cases, the cataloguer hasn't copied any
      // of this field over the desc/title, just leaving them as "--"/"-".
      //
      // Since desc_academic is exposed publicly via Sierra, we can use it
      // here if there's nothing more useful in the other fields.
      val candidateDescription = miroData.description match {
        case Some(s) => {
          if (s == "--" || s == "-") miroData.academicDescription.getOrElse("")
          else s
        }
        case None => ""
      }

      // Populate the title and description.  The rules are as follows:
      //
      //  1.  For V images, if the first line of <image_image_desc> is a
      //      prefix of <image_title>, we use that instead of the title, and
      //      drop the first line of the description.
      //      If it's a single-line description, drop the description entirely.
      //  2.  Otherwise, use the <image_title> ("short description") and
      //      <image_image_desc> ("description") fields.
      //
      // In at least the V collection, many of the titles are truncated forms
      // of the description field -- and we don't want to repeat information
      // in the public API.
      //
      // Note: Every image in the V collection that has image_cleared == Y has
      // non-empty title.  This is _not_ true for the MIRO records in general.
      // TODO: Work out what title to use for those records.
      //
      val candidateTitle = candidateDescription.split("\n").head
      val titleIsTruncatedDescription = candidateTitle
        .startsWith(miroData.title.get)

      val useDescriptionAsTitle = (titleIsTruncatedDescription &&
        MiroCollection == "Images-V") || (miroData.title.get == "-" || miroData.title.get == "--")

      val title =
        if (useDescriptionAsTitle) candidateTitle
        else miroData.title.get

      val description = if (useDescriptionAsTitle) {
        // Remove the first line from the description, and trim any extra
        // whitespace (leading newlines)
        candidateDescription
          .replace(candidateTitle, "")
      } else {
        candidateDescription
      }

      // If the description is an empty string, use a proper None type instead
      val trimmedDescription =
        if (description.trim.length > 0) Some(description.trim) else None

      // <image_creator>: the Creator, which maps to our property "hasCreator"
      val creators: List[Agent] = miroData.creator match {
        case Some(c) => c.map { Agent(_) }
        case None => List()
      }

      // <image_secondary_creator>: what MIRO calls Secondary Creator, which
      // will also just have to map to our object property "hasCreator"
      val secondaryCreators: List[Agent] = miroData.secondaryCreator match {
        case Some(c) => c.map { Agent(_) }
        case None => List()
      }

      // Populate the subjects field.  This is based on two fields in the XML,
      // <image_keywords> and <image_keywords_unauth>.  Both of these were
      // defined in part or whole by the human cataloguers, and in general do
      // not correspond to a controlled vocabulary.  (The latter was imported
      // directly from PhotoSoft.)
      //
      // In some cases, these actually do correspond to controlled vocabs,
      // e.g. where keywords were pulled directly from Sierra -- but we don't
      // have enough information in Miro to determine which ones those are.
      val keywords: List[Concept] = miroData.keywords match {
        case Some(k) => k.map { Concept(_) }
        case None => List()
      }

      val keywordsUnauth: List[Concept] = miroData.keywordsUnauth match {
        case Some(k) => k.map { Concept(_) }
        case None => List()
      }

      val subjects = keywords ++ keywordsUnauth

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

      val genres = physFormat ++ lcGenre

      // Determining the creation date depends on several factors, so we do
      // it on a per-collection basis.
      val createdDate: Option[Period] = MiroCollection match {
        case "Images-V" => miroData.artworkDate.map { Period(_) }
        case _ => None
      }

      // Add a thumbnail.
      val useRestrictions = miroData.useRestrictions match {
        case Some(s) => s
        case None =>
          throw new ShouldNotTransformException(
            field = "image_use_restrictions",
            value = miroData.useRestrictions,
            message = "No value provided for image_use_restrictions?"
          )
      }
      val thumbnail = Location(
        locationType = "thumbnail-image",
        url = Some(buildThumbnailURL(MiroID)),
        license = chooseLicense(useRestrictions = useRestrictions)
      )

      Work(
        identifiers = identifiers,
        title = title,
        description = trimmedDescription,
        createdDate = createdDate,
        creators = creators ++ secondaryCreators,
        subjects = subjects,
        genres = genres,
        thumbnail = Some(thumbnail)
      )
    }
  }

  /** Build a thumbnail URL for the image.
    *
    * TODO: Make the template configurable
    */
  def buildThumbnailURL(miroID: String): String =
    "https://iiif.wellcomecollection.org/image/MIROID.jpg/full/300,/0/default.jpg"
      .replace(
        "MIROID",
        miroID
      )

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
  def chooseLicense(useRestrictions: String): BaseLicense =
    useRestrictions match {

      // Certain strings map directly onto license types
      case "CC-0" => License_CC0
      case "CC-BY" => License_CCBY
      case "CC-BY-NC" => License_CCBYNC
      case "CC-BY-NC-ND" => License_CCBYNCND

      // These mappings are defined in Christy's document
      case "Academics" => License_CCBYNC

      // Any images with this label are explicitly withheld from the API.
      case "See Related Images Tab for Higher Res Available" => {
        throw new ShouldNotTransformException(
          field = "image_use_restrictions",
          value = useRestrictions,
          message = "Images with this license are explicitly excluded"
        )
      }

      // These fields are labelled "[Investigate further]" in Christy's
      // document, so for now we exclude them.
      case ("Do not use" | "Not for external use" |
          "See Copyright Information" | "Suppressed from WI site") => {
        throw new ShouldNotTransformException(
          field = "image_use_restrictions",
          value = useRestrictions,
          message =
            "Images with this license need more investigation before showing in the API"
        )
      }

      case _ =>
        throw new ShouldNotTransformException(
          field = "image_use_restrictions",
          value = useRestrictions,
          message = "This license type is unrecognised"
        )
    }
}
