package uk.ac.wellcome.models

trait Identifiable {
  val sourceIdentifier: SourceIdentifier
  val ontologyType: String
}
