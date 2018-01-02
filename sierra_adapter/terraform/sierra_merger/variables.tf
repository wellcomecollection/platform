variable "resource_type" {}

variable "dynamo_updates_queue_name" {}
variable "dynamo_updates_queue_arn" {}
variable "dynamo_updates_queue_url" {}

variable "target_dynamo_table_name" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "account_id" {}

variable "dlq_alarm_arn" {}

variable "cluster_name" {}

variable "vpc_id" {}

variable "release_id" {}

variable "infra_bucket" {
  default = "platform-infra"
}

variable "alb_listener_https_arn" {}
variable "alb_listener_http_arn" {}
variable "alb_priority" {}
variable "alb_cloudwatch_id" {}

variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}
