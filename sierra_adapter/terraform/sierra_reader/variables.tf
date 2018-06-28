variable "resource_type" {}

variable "bucket_name" {}
variable "windows_topic_name" {}

variable "sierra_fields" {}

variable "sierra_api_url" {}
variable "sierra_oauth_key" {}
variable "sierra_oauth_secret" {}

variable "release_id" {}

variable "cluster_name" {}
variable "vpc_id" {}

variable "dlq_alarm_arn" {}
variable "lambda_error_alarm_arn" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "account_id" {}
variable "infra_bucket" {}

variable "subnets" {
  type = "list"
}

variable "namespace_id" {}
variable "interservice_security_group_id" {}
variable "service_egress_security_group_id" {}

variable "sierra_reader_ecr_repository_url" {}
