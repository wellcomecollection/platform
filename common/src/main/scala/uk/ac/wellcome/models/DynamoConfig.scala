package uk.ac.wellcome.models

case class DynamoConfig(region: String,
                        applicationName: String,
                        arn: String,
                        table: String)
