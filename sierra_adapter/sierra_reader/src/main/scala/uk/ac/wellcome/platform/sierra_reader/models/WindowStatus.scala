package uk.ac.wellcome.platform.sierra_reader.models

import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumbers

case class WindowStatus(id: Option[String], offset: Int)

case object WindowStatus {
  def apply(id: String, offset: Int): WindowStatus = {
    SierraRecordNumbers.assertIs7DigitRecordNumber(id)
    WindowStatus(id = Some(id), offset = offset)
  }
}
