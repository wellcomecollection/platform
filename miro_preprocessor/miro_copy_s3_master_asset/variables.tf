variable "bucket_source_asset_arn" {}

variable "lambda_error_alarm_arn" {}

variable "topic_miro_copy_s3_master_asset_arn" {}

variable "bucket_destination_asset_arn" {}

variable "topic_forward_sns_message_arn" {
  default = ""
}

variable "bucket_source_asset_name" {}

variable "bucket_destination_name" {}

variable "lambda_description" {}

variable "lambda_name" {}

variable "destination_key_prefix" {}
