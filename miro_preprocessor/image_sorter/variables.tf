variable "lambda_error_alarm_arn" {}

variable "topic_cold_store_arn" {
  description = "ARN for the SNS topic for sending to Cold Store"
}

variable "topic_cold_store_publish_policy" {
  description = "JSON policy for pushing to `topic_cold_store_arn`"
}

variable "topic_tandem_vault_arn" {
  description = "ARN for the SNS topic for sending to Tandem Vault"
}

variable "topic_tandem_vault_publish_policy" {
  description = "JSON policy for pushing to `topic_tandem_vault_arn`"
}

variable "topic_catalogue_api_arn" {
  description = "ARN for the SNS topic for sending to the Catalogue API"
}

variable "topic_catalogue_api_publish_policy" {
  description = "JSON policy for pushing to `topic_catalogue_api_arn`"
}

variable "s3_miro_data_id" {}
variable "s3_miro_data_arn" {}
