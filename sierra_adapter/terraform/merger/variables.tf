variable "resource_type" {}

variable "release_id" {}

variable "merged_dynamo_table_name" {}

variable "updates_topic_name" {}

variable "cluster_name" {}
variable "vpc_id" {}

variable "alb_listener_http_arn" {}
variable "alb_listener_https_arn" {}
variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}
variable "alb_cloudwatch_id" {}

variable "dlq_alarm_arn" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "account_id" {}
variable "vhs_full_access_policy" {}
variable "bucket_name" {}