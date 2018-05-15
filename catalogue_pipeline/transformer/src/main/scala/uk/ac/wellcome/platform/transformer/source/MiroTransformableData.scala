package uk.ac.wellcome.platform.transformer.source

import io.circe.generic.extras.JsonKey
import org.apache.commons.lang.StringEscapeUtils
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.{Failure, Success, Try}

case class MiroTransformableData(
  @JsonKey("image_title") title: Option[String],
  // Miro data has null json values scattered a bit everywhere.
  // If the type is not Option, circe decoding fails when it
  // encounters a null. Hence this weird type signature
  @JsonKey("image_creator") creator: Option[List[Option[String]]],
  @JsonKey("image_image_desc") description: Option[String],
  @JsonKey("image_image_desc_academic") academicDescription: Option[String],
  @JsonKey("image_secondary_creator") secondaryCreator: Option[List[String]],
  @JsonKey("image_artwork_date") artworkDate: Option[String],
  @JsonKey("image_cleared") cleared: Option[String],
  @JsonKey("image_copyright_cleared") copyrightCleared: Option[String],
  @JsonKey("image_keywords") keywords: Option[List[String]],
  @JsonKey("image_keywords_unauth") keywordsUnauth: Option[List[String]],
  @JsonKey("image_phys_format") physFormat: Option[String],
  @JsonKey("image_lc_genre") lcGenre: Option[String],
  @JsonKey("image_tech_file_size") techFileSize: Option[List[String]],
  @JsonKey("image_use_restrictions") useRestrictions: Option[String],
  @JsonKey("image_supp_lettering") suppLettering: Option[String],
  @JsonKey("image_innopac_id") innopacID: Option[String],
  @JsonKey("image_credit_line") creditLine: Option[String],
  @JsonKey("image_source_code") sourceCode: Option[String],
  @JsonKey("image_library_ref_department") libraryRefDepartment: List[
    Option[String]] = Nil,
  @JsonKey("image_library_ref_id") libraryRefId: List[Option[String]] = Nil,
  @JsonKey("image_award") award: List[Option[String]] = Nil,
  @JsonKey("image_award_date") awardDate: List[Option[String]] = Nil
)

case object MiroTransformableData {

  /* Some of the Miro fields were imported from Sierra, and had special
   * characters replaced by HTML-encoded entities when copied across.
   *
   * We need to fix them up before we decode as JSON.
   */
  private def unescapeHtml(data: String): String =
    StringEscapeUtils.unescapeHtml(data)

  /* Create MiroTransformableData from string */
  private def createMiroTransformableData(
    data: String): Try[MiroTransformableData] =
    fromJson[MiroTransformableData](data)

  def create(data: String): MiroTransformableData = {
    val unescapedData = unescapeHtml(data)

    val tryMiroTransformableData =
      createMiroTransformableData(unescapedData)

    val miroTransformableData: MiroTransformableData =
      tryMiroTransformableData match {
        case Success(miroData) => miroData
        case Failure(e) => throw e
      }

    miroTransformableData
  }
}
