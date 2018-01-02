variable "resource_type" {}

variable "windows_topic_name" {}

variable "dlq_alarm_arn" {}

variable "cluster_name" {}

variable "vpc_id" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "alb_priority" {}

variable "alb_listener_http_arn" {}
variable "alb_listener_https_arn" {}

variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}

variable "alb_cloudwatch_id" {}

variable "release_id" {}

variable "infra_bucket" {
  default = "platform-infra"
}

variable "account_id" {}

variable "sierra_api_url" {}
variable "sierra_oauth_key" {}
variable "sierra_oauth_secret" {}

variable "sierra_fields" {}

variable "lambda_error_alarm_arn" {}
