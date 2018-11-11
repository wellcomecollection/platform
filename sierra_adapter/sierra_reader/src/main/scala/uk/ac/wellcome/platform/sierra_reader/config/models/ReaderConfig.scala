package uk.ac.wellcome.platform.sierra_reader.config.models

import uk.ac.wellcome.platform.sierra_reader.models.SierraResourceTypes

case class ReaderConfig(
  resourceType: SierraResourceTypes.Value,
  fields: String,
  batchSize: Int = 50
)
