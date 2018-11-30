package uk.ac.wellcome.platform.transformer.miro.transformers

import java.io.InputStream

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.transformer.miro.exceptions.ShouldNotTransformException

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
  private val contributorMap: Map[String, String] =
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
  private val perRecordContributorMap: Map[String, Map[String, String]] =
    toMap[Map[String, String]](Source.fromInputStream(perRecordStream).mkString).get

  // Returns our best guess for the credit line of an image, given its
  // Miro ID and contributor code.
  //
  // We look in the general contributor map first, and if that fails,
  // we fallback to the per-record map.
  //
  def lookupContributorCode(miroId: String, code: String): Option[String] = {

    // All the codes in our map are uppercase, but some of the Miro records
    // use lowercased versions, so we cast everything to ALL CAPS just in case.
    val creditCode = code.toUpperCase()

    // We had a request to remove nine records from contributor GUS from
    // the API after we'd done the initial sort of the Miro data.
    // We removed them from Elasticsearch by hand, but we don't want
    // them to reappear on reindex.
    //
    // Until we can move them to Tandem Vault or Cold Store, we'll throw
    // them away at this point.
    val gusMiroIds = List(
      "B0009891",
      "B0009897",
      "B0009886",
      "B0009893",
      "B0009887",
      "B0009895",
      "B0009884",
      "B0009890",
      "B0009888"
    )

    if (creditCode == "GUS" && gusMiroIds.contains(miroId)) {
      throw new ShouldNotTransformException(
        s"Image $miroId from contributor GUS should not be sent to the pipeline"
      )
    }

    contributorMap.get(creditCode) match {
      case Some(creditLine) => Some(creditLine)
      case None =>
        perRecordContributorMap.get(miroId) match {
          case Some(perRecordMap) => perRecordMap.get(creditCode)
          case None               => None
        }
    }
  }
}
