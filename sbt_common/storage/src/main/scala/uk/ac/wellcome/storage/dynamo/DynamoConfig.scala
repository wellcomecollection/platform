package uk.ac.wellcome.storage.dynamo

case class DynamoConfig(table: String, maybeIndex: Option[String] = None)

case object DynamoConfig {
  def apply(table: String, index: String): DynamoConfig =
    DynamoConfig(
      table = table,
      maybeIndex = Some(index)
    )
}
