variable "lambda_error_alarm_arn" {}
variable "bucket_source_asset_name" {}
variable "bucket_source_asset_arn" {}
variable "topic_miro_image_to_tandem_vault_arn" {}
variable "tandem_vault_api_key" {}
variable "tandem_vault_api_url" {
  default = "https://wellcome.tandemvault.com/api/v1"
}