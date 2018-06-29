variable "name" {}

variable "dynamo_table_names" {
  description = "DynamoDb table names to heartbeat"
  type        = "string"
}

variable "lambda_error_alarm_arn" {}

variable "infra_bucket" {}
