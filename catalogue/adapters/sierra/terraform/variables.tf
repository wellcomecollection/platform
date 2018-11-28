variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "release_ids" {
  type = "map"
}

variable "key_name" {
  description = "Name of AWS key pair"
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

variable "vpc_cidr_block" {
  default = "20.60.0.0/16"
}

variable "critical_slack_webhook" {}
