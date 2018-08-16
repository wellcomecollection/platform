package uk.ac.wellcome.platform.archive.common.models

import uk.ac.wellcome.storage.ObjectLocation

case class BagDigestItem(checksum: String, location: ObjectLocation)
case class BagInfoLine(key: String, value: String)
