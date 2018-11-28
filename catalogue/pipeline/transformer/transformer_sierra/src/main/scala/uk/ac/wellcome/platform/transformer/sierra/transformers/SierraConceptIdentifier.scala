package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}
import uk.ac.wellcome.platform.transformer.sierra.source.VarField

// Implements logic for finding a source identifier for varFields with
// MARC tag 648, 650, 651 and 655.  These are the fields we use for genre
// and subject.
//
// The rules for these identifiers is moderately fiddly:
//
//   - Look in indicator 2.  Values 0 to 6 have hard-coded meanings.
//   - If indicator 2 is 7, look in subfield $2 instead.
//
// The rules are the same for all four MARC tags, hence the shared object.
//
// https://www.loc.gov/marc/bibliographic/bd648.html
// https://www.loc.gov/marc/bibliographic/bd650.html
// https://www.loc.gov/marc/bibliographic/bd651.html
// https://www.loc.gov/marc/bibliographic/bd655.html
//
object SierraConceptIdentifier {

  def maybeFindIdentifier(varField: VarField,
                          identifierSubfieldContent: String,
                          ontologyType: String): Option[SourceIdentifier] = {
    val maybeIdentifierType = varField.indicator2 match {
      case None => None

      // These mappings are provided by the MARC spec.
      // https://www.loc.gov/marc/bibliographic/bd655.html
      case Some("0") => Some(IdentifierType("lc-subjects"))
      case Some("2") => Some(IdentifierType("nlm-mesh"))
      case Some("4") => None

      // For now we omit the other schemes as they're fairly unusual in
      // our collections.  If ind2 = "7", then we need to look in another
      // subfield to find the identifier scheme.  For now, we just highlight
      // LCSH and MESH, and drop everything else.
      // TODO: Revisit this properly.
      case Some(scheme) => None
    }

    maybeIdentifierType match {
      case None => None
      case Some(identifierType) =>
        Some(
          SourceIdentifier(
            identifierType = identifierType,
            value = identifierSubfieldContent,
            ontologyType = ontologyType
          ))
    }
  }
}
