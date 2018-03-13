variable "dlq_alarm_arn" {}

variable "aws_region" {}
variable "account_id" {}

variable "release_ids" {
  type = "map"
}

variable "cluster_name" {}
variable "vpc_id" {}

variable "upload_bucket" {}

variable "es_name" {}
variable "es_region" {}
variable "es_port" {}
variable "es_index" {}
variable "es_doc_type" {}
variable "es_username" {}
variable "es_password" {}

variable "alb_cloudwatch_id" {}
variable "alb_listener_https_arn" {}
variable "alb_listener_http_arn" {}
variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}

variable "schedule_topic_name" {}
