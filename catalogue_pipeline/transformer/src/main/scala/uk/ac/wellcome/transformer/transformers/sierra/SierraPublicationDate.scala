package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.transformer.source.SierraBibData
import uk.ac.wellcome.work_model.Period

trait SierraPublicationDate extends MarcUtils {

  // Populate wwork:publicationDate.
  //
  // We use MARC field "260" and subfield "c".  For now, we just pass through
  // the string directly -- we're not doing any parsing of the date yet.
  //
  // Notes:
  //
  //  - MARC field 260 and subfield $c are both labelled "R" (repeatable).
  //    However, the notes for subfield $c say:
  //
  //        Only one subfield $c may be recorded in multiple publishing
  //        statements of a record.
  //
  //    Because it's unclear, for now we just take them all and join them
  //    with semicolons.
  //
  //    TODO: Decide a proper strategy for doing this!
  //
  //  - This field may contain punctuation based on surrounding MARC fields,
  //    or which explains exactly what the date means.  For now, we treat
  //    this as entirely opaque, and just pipe through the raw string.
  //
  //    TODO: Parse it properly!
  //
  // https://www.loc.gov/marc/bibliographic/bd260.html
  //
  def getPublicationDate(bibData: SierraBibData): Option[Period] = {
    val matchingSubfields = getMatchingSubfields(
      bibData = bibData,
      marcTag = "260",
      marcSubfieldTag = "c"
    ).flatten

    if (matchingSubfields.isEmpty) {
      None
    } else {
      val label = matchingSubfields
        .map { _.content }
        .mkString("; ")
      Some(Period(label))
    }
  }
}
