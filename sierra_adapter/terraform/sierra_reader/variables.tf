variable "resource_type" {}

variable "bucket_name" {}
variable "windows_topic_name" {}

variable "sierra_fields" {}

variable "sierra_api_url" {}
variable "sierra_oauth_key" {}
variable "sierra_oauth_secret" {}

variable "release_id" {}
variable "ecr_repository_url" {}

variable "cluster_name" {}
variable "vpc_id" {}

variable "alb_listener_http_arn" {}
variable "alb_listener_https_arn" {}
variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}
variable "alb_cloudwatch_id" {}

variable "dlq_alarm_arn" {}
variable "lambda_error_alarm_arn" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "account_id" {}
variable "infra_bucket" {}
