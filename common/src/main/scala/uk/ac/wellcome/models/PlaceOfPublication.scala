package uk.ac.wellcome.models

trait PlaceOfPublication

case class UnidentifiablePlaceOfPublication(label: String,
                                            ontologyType: String = "Place")
  extends PlaceOfPublication
case class IdentifiablePlaceOfPublication(label: String,
                                          sourceIdentifier: SourceIdentifier,
                                          identifiers: List[SourceIdentifier],
                                          ontologyType: String = "Place")
  extends PlaceOfPublication
    with Identifiable
