package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.transformer.source.SierraBibData

trait SierraExtent extends MarcUtils {

  // Populate wwork:extent.
  //
  // We use MARC field 300 and subfield $a.
  //
  // Notes:
  //
  //  - MARC field 300 and subfield $a are both labelled "R" (repeatable),
  //    and include examples of it being repeated.
  //
  //  - This field is usually meant to be joined with other fields, with
  //    display logic that isn't entirely clear.
  //
  //    So far we don't do any stripping of punctuation, and if multiple
  //    subfields are found on a record, I'm just joining them with spaces.
  //
  //    TODO: Decide a proper strategy for joining multiple physical
  //    descriptions!
  //
  // https://www.loc.gov/marc/bibliographic/bd300.html
  //
  def getExtent(bibData: SierraBibData): Option[String] = {
    val matchingSubfields = getMatchingSubfields(
      bibData = bibData,
      marcTag = "300",
      marcSubfieldTag = "a"
    )

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
