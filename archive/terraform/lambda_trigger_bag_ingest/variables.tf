variable "name" {}

variable "bag_paths" {}

variable "account_id" {}

variable "storage_space" {
  default = "test"
}

variable "api_url" {
  default = "https://api.wellcomecollection.org"
}

variable "oauth_details_enc" {}

variable "lambda_error_alarm_arn" {}

variable "infra_bucket" {}

variable "lambda_s3_key" {}

variable "ingest_bucket_name" {}
