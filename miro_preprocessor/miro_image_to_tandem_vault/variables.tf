variable "lambda_error_alarm_arn" {}
variable "bucket_source_asset_name" {}
variable "bucket_source_asset_arn" {}
variable "topic_miro_image_to_tandem_vault_arn" {}
variable "topic_miro_image_to_tandem_vault_name" {}
variable "tandem_vault_api_key" {}

variable "tandem_vault_api_url" {
  default = "https://wellcome.tandemvault.com/api/v1"
}

variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "account_id" {
  description = "The AWS account Id"
}

variable "dlq_alarm_arn" {
  description = "ARN of the DQL action resource"
}

variable "release_ids" {
  type = "map"
}
