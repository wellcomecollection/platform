package uk.ac.wellcome.platform.sierra_reader.models

import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber

case class WindowStatus(id: Option[SierraRecordNumber], offset: Int)
