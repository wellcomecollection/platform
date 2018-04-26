package uk.ac.wellcome.work_model

/** An identifier received from one of the original sources */
case class SourceIdentifier(
  identifierScheme: IdentifierSchemes.IdentifierScheme,
  ontologyType: String,
  value: String)
