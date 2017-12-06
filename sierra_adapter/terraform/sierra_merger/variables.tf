variable "resource_type" {}

variable "dynamo_events_topic_name" {}

variable "target_dynamo_table_name" {}
variable "target_dynamo_table_arn" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "account_id" {}

variable "dlq_alarm_arn" {}

variable "cluster_name" {}

variable "vpc_id" {}

variable "ecr_repository_url" {}
variable "release_id" {}

variable "build_env" {
  default = "prod"
}

variable "infra_bucket" {
  default = "platform-infra"
}

variable "alb_listener_https_arn" {}
variable "alb_listener_http_arn" {}
variable "alb_priority" {}
variable "alb_cloudwatch_id" {}

variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}
