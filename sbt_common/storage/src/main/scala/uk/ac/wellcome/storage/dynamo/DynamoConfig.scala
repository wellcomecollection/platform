package uk.ac.wellcome.storage.dynamo

case class DynamoConfig(table: String, index: Option[String])
