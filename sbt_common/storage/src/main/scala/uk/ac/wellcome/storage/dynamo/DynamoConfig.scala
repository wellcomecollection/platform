package uk.ac.wellcome.storage.dynamo

import javax.naming.ConfigurationException

case class DynamoConfig(table: String, maybeIndex: Option[String] = None) {
  def index: String = maybeIndex.getOrElse(
    throw new ConfigurationException(
      "Tried to look up the index, but no index is configured!"
    )
  )
}

case object DynamoConfig {
  def apply(table: String, index: String): DynamoConfig =
    DynamoConfig(
      table = table,
      maybeIndex = Some(index)
    )
}
