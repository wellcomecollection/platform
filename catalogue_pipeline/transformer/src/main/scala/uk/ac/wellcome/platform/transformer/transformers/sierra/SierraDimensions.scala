package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.platform.transformer.source.SierraBibData

trait SierraDimensions extends MarcUtils {

  // Populate wwork:dimensions.
  //
  // We use MARC field 300 and subfield $c.
  //
  // Notes:
  //
  //  - MARC field 300 and subfield $c are both labelled "R" (repeatable),
  //    and the docs include examples of it being repeated (e.g. for music).
  //    For now, multiple instances are just joined with spaces.
  //
  //    TODO: Decide a proper strategy for joining multiple dimensions.
  //
  //  - The MARC spec has notes on trailing punctuation:
  //
  //        In records formulated according to ISBD principles, subfield $c
  //        contains all data following a semicolon (;) and up to and including
  //        the next mark of ISBD punctuation, if any (e.g., a plus sign (+)).
  //
  //    Currently we pass this all through.
  //
  //    TODO: Clean up trailing punctuation as appropriate.
  //
  //  - Silver's notes include an example of dimensions where quantity and
  //    unit are pulled out separately, but that would need us to parse the
  //    strings -- MARC doesn't expose this directly.
  //
  //    TODO: Find a way to implement this.
  //
  // https://www.loc.gov/marc/bibliographic/bd300.html
  //
  def getDimensions(bibData: SierraBibData): Option[String] = {
    val matchingSubfields = getMatchingSubfields(
      bibData = bibData,
      marcTag = "300",
      marcSubfieldTag = "c"
    ).flatten

    if (matchingSubfields.isEmpty) {
      None
    } else {
      val label = matchingSubfields
        .map { _.content }
        .mkString(" ")
      Some(label)
    }
  }
}
