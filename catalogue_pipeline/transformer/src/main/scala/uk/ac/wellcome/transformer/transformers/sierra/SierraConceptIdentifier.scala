package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.{IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.transformer.source.{MarcSubfield, VarField}

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

  def maybeFindIdentifier(
    varField: VarField,
    identifierSubfield: MarcSubfield,
    ontologyType: String): Option[SourceIdentifier] = {

    // The mapping from indicator 2 to the identifier scheme is provided
    // by the MARC spec.
    // https://www.loc.gov/marc/bibliographic/bd655.html
    val maybeIdentifierScheme = varField.indicator2 match {
      case None => None
      case Some("0") =>
        Some(IdentifierSchemes.libraryOfCongressSubjectHeadings)
      case Some("2") => Some(IdentifierSchemes.medicalSubjectHeadings)
      case Some(scheme) =>
        throw new RuntimeException(s"Unrecognised identifier scheme: $scheme (${varField.subfields})")
    }

    maybeIdentifierScheme match {
      case None => None
      case Some(identifierScheme) =>
        Some(SourceIdentifier(
          identifierScheme = identifierScheme,
          value = identifierSubfield.content,
          ontologyType = ontologyType
        ))
    }
  }
}
