package uk.ac.wellcome.transformer.source

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang.StringEscapeUtils
import uk.ac.wellcome.utils.JsonUtil

import scala.util.{Failure, Success, Try}

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
  @JsonProperty("image_use_restrictions") useRestrictions: Option[String],
  @JsonProperty("image_supp_lettering") suppLettering: Option[String],
  @JsonProperty("image_innopac_id") innopacID: Option[String],
  @JsonProperty("image_credit_line") creditLine: Option[String],
  @JsonProperty("image_source_code") sourceCode: Option[String],
  @JsonProperty("image_library_ref_department") libraryRefDepartment: Option[
    List[String]],
  @JsonProperty("image_library_ref_id") libraryRefId: Option[List[String]],
  @JsonProperty("image_award") award: Option[List[String]],
  @JsonProperty("image_award_date") awardDate: Option[List[String]]
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
    JsonUtil.fromJson[MiroTransformableData](data)

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
