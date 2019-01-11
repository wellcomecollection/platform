variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "release_ids" {
  type = "map"
}

variable "sierra_api_url" {}
variable "sierra_oauth_key" {}
variable "sierra_oauth_secret" {}

variable "sierra_bibs_fields" {}
variable "sierra_items_fields" {}

variable "infra_bucket" {}

variable "namespace" {
  default = "sierra-adapter"
}

variable "critical_slack_webhook" {}
