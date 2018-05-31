package uk.ac.wellcome.platform.matcher.models

import uk.ac.wellcome.models.matcher.EquivalentIdentifiers

// A1 A2 A3
// B1 B2
// C1 C2 C3

// ^ three merged works
//
// list of these lists
// passed on to merger

// to new namespace

case class LinkedWorksIdentifiersList(linkedWorks: Set[EquivalentIdentifiers])
