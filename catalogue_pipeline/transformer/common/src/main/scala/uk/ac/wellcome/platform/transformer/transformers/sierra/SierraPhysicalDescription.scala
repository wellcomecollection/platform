package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.platform.transformer.source.SierraBibData

trait SierraPhysicalDescription extends MarcUtils {

  // Populate wwork:physicalDescription.
  //
  // We use MARC field 300 and subfield $b.
  //
  // Notes:
  //
  //  - MARC field 300 and subfield $b are both labelled "R" (repeatable).
  //    According to Branwen, this field does appear multiple times on some
  //    of our records -- not usually on books, but on some of the moving image
  //    & sound records.
  //
  //  - So far we don't do any stripping of punctuation, and if multiple
  //    subfields are found on a record, I'm just joining them with newlines.
  //
  //    TODO: Decide a proper strategy for joining multiple physical
  //    descriptions!
  //
  // https://www.loc.gov/marc/bibliographic/bd300.html
  //
  def getPhysicalDescription(bibData: SierraBibData): Option[String] = {
    val matchingSubfields = getMatchingSubfields(
      bibData = bibData,
      marcTag = "300",
      marcSubfieldTag = "b"
    ).flatten

    if (matchingSubfields.isEmpty) {
      None
    } else {
      val label = matchingSubfields
        .map { _.content }
        .mkString("\n\n")
      Some(label)
    }
  }
}
