variable "resource_type" {}

variable "window_length_minutes" {}
variable "window_interval_minutes" {}

variable "release_id" {
  type = "map"
}

variable "sierra_api_url" {}
variable "sierra_oauth_key" {}
variable "sierra_oauth_secret" {}
variable "sierra_fields" {}

variable "sierradata_table_name" {}

variable "lambda_error_alarm_arn" {}
variable "dlq_alarm_arn" {}

variable "alb_listener_http_arn" {}
variable "alb_listener_https_arn" {}
variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}
variable "alb_cloudwatch_id" {}

variable "cluster_name" {}
variable "vpc_id" {}

variable "account_id" {}
