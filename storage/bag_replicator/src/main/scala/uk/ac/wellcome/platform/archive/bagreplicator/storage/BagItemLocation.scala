package uk.ac.wellcome.platform.archive.bagreplicator.storage

import uk.ac.wellcome.platform.archive.common.models.BagLocation

// TODO: Move this into top level later
case class BagItemLocation(bagLocation: BagLocation, itemPath: String)
