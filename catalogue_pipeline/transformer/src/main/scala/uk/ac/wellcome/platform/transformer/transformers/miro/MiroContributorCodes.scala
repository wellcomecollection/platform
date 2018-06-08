package uk.ac.wellcome.platform.transformer.transformers.miro

import java.io.InputStream

import uk.ac.wellcome.utils.JsonUtil.toMap

import scala.io.Source

trait MiroContributorCodes {
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
  private val stream: InputStream = getClass
    .getResourceAsStream("/miro_contributor_map.json")
  val contributorMap: Map[String, String] =
    toMap[String](Source.fromInputStream(stream).mkString).get

  // This JSON resource gives us contributor codes on a per-record basis.
  //
  // For some contributors, the contributor code resolved to phrases like:
  //
  //      See notes
  //      [Contributor name], University of X
  //
  // Because that's only a small number of records, we have a second map
  // that tells us what contributor codes mean on a per-record basis.
  private val perRecordStream: InputStream = getClass
    .getResourceAsStream("/miro_individual_record_contributor_map.json")
  val perRecordContributorMap: Map[String, Map[String, String]] =
    toMap[Map[String, String]](
      Source.fromInputStream(perRecordStream).mkString).get

  // Returns our best guess for the credit line of an image, given its
  // Miro ID and contributor code.
  //
  // We look in the general contributor map first, and if that fails,
  // we fallback to the per-record map.
  //
  def lookupContributorCode(miroId: String, code: String): Option[String] =
    contributorMap.get(code) match {
      case Some(creditLine) => Some(creditLine)
      case None => {
        perRecordContributorMap.get(miroId) match {
          case Some(perRecordMap) => perRecordMap.get(code)
          case None => None
        }
      }
    }
}
