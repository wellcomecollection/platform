variable "name" {}

variable "dynamo_table_names" {
  description = "DynamoDb table names to heartbeat"
  type        = "list"
}

variable "lambda_error_alarm_arn" {}

variable "infra_bucket" {}
