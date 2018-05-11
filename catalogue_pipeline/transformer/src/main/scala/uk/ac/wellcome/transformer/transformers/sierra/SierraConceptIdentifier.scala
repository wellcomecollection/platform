package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.{IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.transformer.source.{MarcSubfield, VarField}

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
        throw new RuntimeException(s"Unrecognised identifier scheme: $scheme")
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
