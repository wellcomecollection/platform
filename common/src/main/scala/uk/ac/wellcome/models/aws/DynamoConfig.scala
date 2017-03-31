package uk.ac.wellcome.models.aws

case class DynamoConfig(region: String,
                        applicationName: String,
                        arn: String,
                        table: String)
