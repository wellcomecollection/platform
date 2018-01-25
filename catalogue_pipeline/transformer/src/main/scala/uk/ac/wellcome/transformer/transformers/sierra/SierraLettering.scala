package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.transformer.source.SierraBibData

trait SierraLettering {

  // Populate wwork:lettering.
  //
  // We use the prefilled "lettering" field, if there's a MARC field 246
  // with indicator 16.  Otherwise nothing.
  //
  // Notes:
  //  - If a record is an archive, image or a journal, there are different
  //    rules (Silver's notes say "look at 749 but not subfield 6").  We don't
  //    have a way to distinguish these yet, so for now we don't do anything.
  //    TODO: When we know how to identify these, implement the correct rules
  //    for lettering.
  //
  def getLettering(bibData: SierraBibData): Option[String] = {
    val matchingFields = bibData.varFields
      .filter { _.marcTag == Some("246") }
      .filter { _.indicator1 == Some("1") }
      .filter { _.indicator2 == Some("6") }

    if (matchingFields.isEmpty) {
      None
    } else {
      bibData.lettering
    }
  }
}
