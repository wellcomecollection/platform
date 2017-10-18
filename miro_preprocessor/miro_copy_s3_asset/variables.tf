variable "bucket_source_asset_arn" {}

variable "lambda_error_alarm_arn" {}

variable "topic_miro_copy_s3_asset_arn" {}

variable "bucket_destination_asset_arn" {}

variable "topic_forward_sns_message_arn" {
  description = "ARN of the SNS topic where to forward the SNS message. Only used if it's a derivative copy."
  default     = ""
}

variable "bucket_source_asset_name" {}

variable "bucket_destination_name" {}

variable "lambda_description" {}

variable "lambda_name" {}

variable "is_master_asset" {}

variable "destination_key_prefix" {}
