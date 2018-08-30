package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra

/** For merging Miro/Sierra works, we look in MARC tag 962 subfield $u.
  *
  * Unfortunately, this doesn't contain just the Miro ID, which is what we
  * want -- it contains a Wellcome Images URL, and they're a bit inconsistent.
  * This trait provides a method for extracting the Miro ID from a WI URL,
  * if possible.
  *
  */
trait WellcomeImagesURLParser {
  def maybeGetMiroID(url: String): Option[String] = None
}
