package uk.ac.wellcome.platform.archive.bagreplicator.storage

import uk.ac.wellcome.platform.archive.common.models.FuzzyWuzzy

case class BagItemLocation(bagLocation: FuzzyWuzzy, itemPath: String)
