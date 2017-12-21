package uk.ac.wellcome.models.transformable

import java.io.InputStream

import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import uk.ac.wellcome.models.transformable.miro.MiroTransformableData
import uk.ac.wellcome.models._
import uk.ac.wellcome.utils.JsonUtil
import cats.syntax.functor._

import io.circe.generic.extras.auto._

import scala.io.Source
import scala.util.{Success, Try}

sealed trait Transformable {
  def transform: Try[Option[Work]]
}

object Transformable {
  import uk.ac.wellcome.circe._

  implicit val decodeEvent: Decoder[Transformable] =
    List[Decoder[Transformable]](
      Decoder[CalmTransformable].widen,
      Decoder[MergedSierraRecord].widen,
      Decoder[MiroTransformable].widen
    ).reduceLeft(_ or _)
}

//TODO add some tests around transformation
case class CalmTransformable(
  RecordID: String,
  RecordType: String,
  AltRefNo: String,
  RefNo: String,
  data: String,
  ReindexShard: String = "default",
  ReindexVersion: Int = 0
) extends Transformable
    with Reindexable[String] {

  val id: ItemIdentifier[String] = ItemIdentifier(
    HashKey("RecordID", RecordID),
    RangeKey("RecordType", RecordType)
  )

  def transform: Try[Option[Work]] =
    JsonUtil
      .fromJson[CalmTransformableData](data)
      .flatMap(_.transform)

}

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
        .filter {
          case (label, _) =>
            (label == "WIA Overall Winner" ||
              label == "Wellcome Image Awards" ||
              label == "Biomedical Image Awards")
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
    val secondaryCreators: List[Agent] = miroData.secondaryCreator match {
      case Some(c) => c.map { Agent(_) }
      case None => List()
    }

    // We also add the contributor code for the non-historical images, but
    // only if the contributor *isn't* Wellcome Collection.v
    val contributorCreators: List[Agent] = miroData.sourceCode match {
      case Some(code) =>
        contributorMap(code.toUpperCase) match {
          case "Wellcome Collection" => List()
          case contributor => List(Agent(contributor))
        }
      case None => List()
    }

    primaryCreators ++ secondaryCreators ++ contributorCreators
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

  // This JSON resource gives us credit lines for contributor codes.
  //
  // It is constructed as a map with fields drawn from the `contributors.xml`
  // export from Miro, with:
  //
  //     - `contributor_id` as the key
  //     - `contributor_credit_line` as the value
  //
  // Note that the checked-in file has had some manual edits for consistency,
  // and with a lot of the Wellcome-related strings replaced with
  // "Wellcome Collection".  There are also a handful of manual edits
  // where the fields in Miro weren't filled in correctly.
  val stream: InputStream = getClass
    .getResourceAsStream("/miro_contributor_map.json")
  val contributorMap =
    JsonUtil.toMap[String](Source.fromInputStream(stream).mkString).get

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
          .replaceAll("Adrian Wressell, Heart of England NHSFT",
                      "Adrian Wressell, Heart of England NHS FT")
          .replaceAll("Andrew Dilley,Jane Greening & Bruce Lynn",
                      "Andrew Dilley, Jane Greening & Bruce Lynn")
          .replaceAll("Andrew Dilley,Nicola DeLeon & Bruce Lynn",
                      "Andrew Dilley, Nicola De Leon & Bruce Lynn")
          .replaceAll("Ashley Prytherch, Royal Surrey County Hospital NHS Foundation Trust",
                      "Ashley Prytherch, Royal Surrey County Hospital NHS FT")
          .replaceAll("David Gregory & Debbie Marshall",
                      "David Gregory and Debbie Marshall")
          .replaceAll("David Gregory&Debbie Marshall",
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
        sourceIdentifier =
          SourceIdentifier(IdentifierSchemes.miroImageNumber, MiroID),
        identifiers = List(
          SourceIdentifier(IdentifierSchemes.miroImageNumber, MiroID)
        ),
        locations = List(
          Location(
            locationType = "iiif-image",
            url = Some(buildImageApiURL(MiroID, "info")),
            credit = getCredit(miroData),
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
        //
        // Regex explanation:
        //
        //    ^                 start of string
        //    (?:\.?[bB])?      non-capturing group, which trims 'b' or 'B'
        //                      or '.b' or '.B' from the start of the string,
        //                      but *not* a lone '.'
        //    ([0-9]{7})        capturing group, the 7 digits of the system
        //                      number
        //    [0-9xX]           the final check digit, which may be X
        //    $                 end of the string
        //
        val regexMatch = """^(?:\.?[bB])?([0-9]{7})[0-9xX]$""".r.unapplySeq(s)
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

    // Add any other legacy identifiers to this record.  Right now we
    // put them all in the same identifier scheme, because we're not doing
    // any transformation or cleaning.
    val libraryRefsList: List[SourceIdentifier] =
      zipMiroFields(keys = miroData.libraryRefDepartment,
                    values = miroData.libraryRefId)
        .map {
          case (label, value) =>
            SourceIdentifier(
              IdentifierSchemes.miroLibraryReference,
              s"$label $value"
            )
        }

    miroIDList ++ sierraList ++ libraryRefsList
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
  def zipMiroFields(keys: Option[List[String]],
                    values: Option[List[String]]): List[(String, String)] = {
    (keys, values) match {
      case (Some(k), Some(v)) => {
        if (k.length != v.length) {
          throw new RuntimeException(
            s"Different lengths! keys=$k, values=$v"
          )
        }

        (k, v).zipped.toList
      }

      // If both fields are empty, we fall straight through.
      case (None, None) => List()

      // If only one of the fields is non-empty, for now we just raise
      // an exception -- this probably indicates an issue in the source data.
      case (Some(k), None) =>
        throw new RuntimeException(
          s"Inconsistent k/v pairs: keys=$k, values=null"
        )
      case (None, Some(v)) =>
        throw new RuntimeException(
          s"Inconsistent k/v pairs: keys=null, values=$v"
        )
    }
  }

  override def transform: Try[Option[Work]] = Try {

    val miroData = MiroTransformableData.create(data)
    val (title, description) = getTitleAndDescription(miroData)

    Some(
      Work(
        sourceIdentifier =
          SourceIdentifier(IdentifierSchemes.miroImageNumber, MiroID),
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
      ))
  }
}

/** Represents a row in the DynamoDB database of "merged" Sierra records;
  * that is, records that contain data for both bibs and
  * their associated items.
  *
  * Fields:
  *
  *   - `id`: the ID of the associated bib record
  *   - `maybeBibData`: data from the associated bib.  This may be None if
  *     we've received an item but haven't had the bib yet.
  *   - `itemData`: a map from item IDs to item records
  *   - `version`: used to track updates to the record in DynamoDB.  The exact
  *     value at any time is unimportant, but it should only ever increase.
  *
  */
case class MergedSierraRecord(
  id: String,
  maybeBibData: Option[SierraBibRecord] = None,
  itemData: Map[String, SierraItemRecord] = Map[String, SierraItemRecord](),
  version: Int = 0
) extends Transformable {

  /** Return the most up-to-date combination of the merged record and the
    * bib record we've just received.
    */
  def mergeBibRecord(record: SierraBibRecord): MergedSierraRecord = {
    if (record.id != this.id) {
      throw new RuntimeException(
        s"Non-matching bib ids ${record.id} != ${this.id}")
    }

    val isNewerData = this.maybeBibData match {
      case Some(bibData) => record.modifiedDate.isAfter(bibData.modifiedDate)
      case None => true
    }

    if (isNewerData) {
      this.copy(maybeBibData = Some(record))
    } else {
      this
    }
  }

  /** Given a new item record, construct the new merged row that we should
    * insert into the merged database.
    *
    * Returns the merged record.
    */
  def mergeItemRecord(itemRecord: SierraItemRecord): MergedSierraRecord = {
    if (!itemRecord.bibIds.contains(this.id)) {
      throw new RuntimeException(
        s"Non-matching bib id ${this.id} in item bib ${itemRecord.bibIds}")
    }

    // We can decide whether to insert the new data in two steps:
    //
    //  - Do we already have any data for this item?  If not, we definitely
    //    need to merge this record.
    //  - If we have existing data, is it newer or older than the update we've
    //    just received?  If the existing data is older, we need to merge the
    //    new record.
    //
    val isNewerData = this.itemData.get(itemRecord.id) match {
      case Some(existing) =>
        itemRecord.modifiedDate.isAfter(existing.modifiedDate)
      case None => true
    }

    if (isNewerData) {
      val itemData = this.itemData + (itemRecord.id -> itemRecord)
      this.copy(itemData = itemData)
    } else {
      this
    }
  }

  def unlinkItemRecord(itemRecord: SierraItemRecord): MergedSierraRecord = {
    if (!itemRecord.unlinkedBibIds.contains(this.id)) {
      throw new RuntimeException(
        s"Non-matching bib id ${this.id} in item unlink bibs ${itemRecord.unlinkedBibIds}")
    }

    val itemData: Map[String, SierraItemRecord] = this.itemData
      .filterNot {
        case (id, currentItemRecord) => {
          val matchesCurrentItemRecord = id == itemRecord.id

          val modifiedAfter = itemRecord.modifiedDate.isAfter(
            currentItemRecord.modifiedDate
          )

          matchesCurrentItemRecord && modifiedAfter
        }
      }

    this.copy(itemData = itemData)
  }

  override def transform: Try[Option[Work]] =
    maybeBibData
      .map { bibData =>
        JsonUtil.fromJson[SierraBibData](bibData.data).map { sierraBibData =>
          Some(Work(
            title = sierraBibData.title,
            sourceIdentifier = SourceIdentifier(
              identifierScheme = IdentifierSchemes.sierraSystemNumber,
              sierraBibData.id
            ),
            identifiers = List(
              SourceIdentifier(
                identifierScheme = IdentifierSchemes.sierraSystemNumber,
                sierraBibData.id
              )
            )
          ))
        }
      }
      .getOrElse(Success(None))
}

case class SierraBibData(id: String, title: String)

object MergedSierraRecord {
  def apply(id: String, bibData: String): MergedSierraRecord = {
    val bibRecord = JsonUtil.fromJson[SierraBibRecord](bibData).get
    MergedSierraRecord(id = id, maybeBibData = Some(bibRecord))
  }

  def apply(bibRecord: SierraBibRecord): MergedSierraRecord =
    MergedSierraRecord(id = bibRecord.id, maybeBibData = Some(bibRecord))

  def apply(id: String, itemRecord: SierraItemRecord): MergedSierraRecord =
    MergedSierraRecord(id = id, itemData = Map(itemRecord.id -> itemRecord))

  def apply(bibRecord: SierraBibRecord, version: Int): MergedSierraRecord =
    MergedSierraRecord(
      id = bibRecord.id,
      maybeBibData = Some(bibRecord),
      version = version
    )
}
