package uk.ac.wellcome.transformer.source

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import org.apache.commons.lang.StringEscapeUtils
import uk.ac.wellcome.circe.jsonUtil._

import scala.util.{Failure, Success, Try}

case class MiroTransformableData(
  @JsonKey("image_title") @JsonProperty("image_title") title: Option[String],
  @JsonKey("image_creator") @JsonProperty("image_creator") creator: Option[List[String]],
  @JsonKey("image_image_desc") @JsonProperty("image_image_desc") description: Option[String],
  @JsonKey("image_image_desc_academic") @JsonProperty("image_image_desc_academic") academicDescription: Option[String],
  @JsonKey("image_secondary_creator") @JsonProperty("image_secondary_creator") secondaryCreator: Option[List[String]],
  @JsonKey("image_artwork_date") @JsonProperty("image_artwork_date") artworkDate: Option[String],
  @JsonKey("image_cleared") @JsonProperty("image_cleared") cleared: Option[String],
  @JsonKey("image_copyright_cleared") @JsonProperty("image_copyright_cleared") copyrightCleared: Option[String],
  @JsonKey("image_keywords") @JsonProperty("image_keywords") keywords: Option[List[String]],
  @JsonKey("image_keywords_unauth") @JsonProperty("image_keywords_unauth") keywordsUnauth: Option[List[String]],
  @JsonKey("image_phys_format") @JsonProperty("image_phys_format") physFormat: Option[String],
  @JsonKey("image_lc_genre") @JsonProperty("image_lc_genre") lcGenre: Option[String],
  @JsonKey("image_tech_file_size") @JsonProperty("image_tech_file_size") techFileSize: Option[List[String]],
  @JsonKey("image_use_restrictions") @JsonProperty("image_use_restrictions") useRestrictions: Option[String],
  @JsonKey("image_supp_lettering") @JsonProperty("image_supp_lettering") suppLettering: Option[String],
  @JsonKey("image_innopac_id") @JsonProperty("image_innopac_id") innopacID: Option[String],
  @JsonKey("image_credit_line") @JsonProperty("image_credit_line") creditLine: Option[String],
  @JsonKey("image_source_code") @JsonProperty("image_source_code") sourceCode: Option[String],
  @JsonKey("image_library_ref_department") @JsonProperty("image_library_ref_department") libraryRefDepartment: Option[List[String]],
  @JsonKey("image_library_ref_id") @JsonProperty("image_library_ref_id") libraryRefId: Option[List[String]],
  @JsonKey("image_award") @JsonProperty("image_award") award: Option[List[String]],
  @JsonKey("image_award_date") @JsonProperty("image_award_date") awardDate: Option[List[String]]
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
