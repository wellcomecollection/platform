package uk.ac.wellcome.models

sealed trait UnidentifiedOrUnidentifieablePlaceOfPublication

sealed trait IdentifiedOrUnidentifiablePlaceOfPublication

case class UnidentifiablePlaceOfPublication(label: String,
                                            ontologyType: String = "Place")
  extends UnidentifiedOrUnidentifieablePlaceOfPublication with IdentifiedOrUnidentifiablePlaceOfPublication

case class UnidentifiedPlaceOfPublication(label: String,
                                          sourceIdentifier: SourceIdentifier,
                                          identifiers: List[SourceIdentifier],
                                          ontologyType: String = "Place")
  extends UnidentifiedOrUnidentifieablePlaceOfPublication
    with Identifiable

case class IdentifiedPlaceOfPublication(label: String,
                                        canonicalId: String,
                                        sourceIdentifier: SourceIdentifier,
                                        identifiers: List[SourceIdentifier],
                                        ontologyType: String = "Place") extends IdentifiedOrUnidentifiablePlaceOfPublication
