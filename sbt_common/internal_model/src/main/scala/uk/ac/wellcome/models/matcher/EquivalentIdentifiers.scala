package uk.ac.wellcome.models.matcher

// Represents a collection of identifiers that represent the same Work.
//
// The identifiers in this set should be merged together.
//
// For example, if the matcher sends EquivalentIdentifiers({A, B, C}), it
// means the merger should combine these into a single work.
//
case class EquivalentIdentifiers(identifiers: Set[String])
