package uk.ac.wellcome.models

/** An identifier received from one of the original sources */
case class SourceIdentifier(
  identifierScheme: IdentifierSchemes.IdentifierScheme,
  ontologyType: String,
  value: String)
