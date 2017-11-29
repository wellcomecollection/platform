package uk.ac.wellcome.models.aws

case class DynamoConfig(arn: String, table: String)

case object DynamoConfig {
  def findWithTable(configs: List[DynamoConfig]): DynamoConfig =
    configs
      .find(_.table.nonEmpty)
      .getOrElse(
        throw new RuntimeException("No configured dynamo tables found"))
}
