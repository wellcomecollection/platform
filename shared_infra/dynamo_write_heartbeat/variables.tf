variable "name" {}

variable "heartbeat_json_config" {
  description = "DynamoDb table names and indices to heartbeat, list of maps, [ { '__heartbeat__': True, 'table_name': 'Table1','key': {'id': {'S': 'heartbeat-id'}} }]"
  type        = "list"
}

variable "lambda_error_alarm_arn" {}

variable "infra_bucket" {}
