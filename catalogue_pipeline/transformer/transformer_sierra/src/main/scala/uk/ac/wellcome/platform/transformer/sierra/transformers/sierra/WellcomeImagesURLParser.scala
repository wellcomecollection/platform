package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra

import scala.util.matching.Regex

/** For merging Miro/Sierra works, we look in MARC tag 962 subfield $u.
  *
  * Unfortunately, this doesn't contain just the Miro ID, which is what we
  * want -- it contains a Wellcome Images URL, and they're a bit inconsistent.
  * This trait provides a method for extracting the Miro ID from a WI URL,
  * if possible.
  *
  */
trait WellcomeImagesURLParser {

  // Regex for parsing a Miro ID.  Examples of valid IDs, all taken from
  // the Sierra data:
  //
  //    L0046161, V0033167F1, V0032544ECL
  //
  private val miroID: Regex = "[A-Z][0-9]{7}[A-Z]{0,3}[0-9]?".r

  // One of the forms of a Wellcome Images URL.  Examples:
  //
  //    http://wellcomeimages.org/indexplus/image/L0046161.html
  //    http://wellcomeimages.org/indexplus/image/L0041574.html.html
  //
  private val indexPlusURL: Regex = s"^http://wellcomeimages\\.org/indexplus/image/($miroID)(?:\\.html){0,2}$$".r.anchored

  def maybeGetMiroID(url: String): Option[String] = url match {
    case indexPlusURL(miroID) => Some(miroID)
    case _ => None
  }
}
