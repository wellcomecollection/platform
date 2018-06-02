package uk.ac.wellcome.models.matcher

// Represents the output from the matcher.
//
// Each entry in the set of works is a collection of identifiers, each of
// which should be merged into a single work.
//
// For example, if we had the result:
//
//    MatchResult([
//      {A1, A2, A3},
//      {B1, B2},
//      {C1, C2, C3}
//    ])
//
// then the merger should create three works, one from A1-A2-A3, a second
// from B1-B2, a third from C1-C2-C3.
//
case class MatcherResult(works: Set[MatchedIdentifiers])
