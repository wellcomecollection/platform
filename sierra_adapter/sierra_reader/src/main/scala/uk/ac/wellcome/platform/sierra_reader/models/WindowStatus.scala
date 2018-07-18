package uk.ac.wellcome.platform.sierra_reader.models

import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber

case class WindowStatus(id: Option[SierraRecordNumber], offset: Int)

case object WindowStatus {
  def apply(id: Option[String], offset: Int): WindowStatus =
    WindowStatus(
      id = id.map { SierraRecordNumber },
      offset = offset
    )
}
