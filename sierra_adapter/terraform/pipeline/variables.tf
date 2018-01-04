variable "resource_type" {}

variable "window_length_minutes" {}
variable "trigger_interval_minutes" {}

variable "sierra_to_dynamo_release_id" {}
variable "sierra_merger_release_id" {}

variable "merged_dynamo_table_name" {}

variable "sierra_api_url" {}
variable "sierra_oauth_key" {}
variable "sierra_oauth_secret" {}
variable "sierra_fields" {}

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
