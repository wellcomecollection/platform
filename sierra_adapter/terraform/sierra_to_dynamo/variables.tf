variable "resource_type" {}

variable "windows_topic_arn" {}

variable "dlq_alarm_arn" {}

variable "cluster_name" {}
variable "cluster_id" {}

variable "vpc_id" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "build_env" {
  default = "prod"
}

variable "alb_listener_http_arn" {}
variable "alb_listener_https_arn" {}

variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}

variable "ecr_repository_url" {}
variable "release_id" {}
