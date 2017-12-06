variable "cluster_name" {}
variable "source_queue_name" {}
variable "name" {}
variable "vpc_id" {}
variable "ecr_repository_url" {}
variable "release_id" {}

variable "infra_bucket" {
  default = "platform-infra"
}

variable "config_template" {
  default = ""
}

variable "cpu" {
  default = 256
}

variable "memory" {
  default = 1024
}

variable "config_vars" {
  type = "map"
}

variable "build_env" {
  default = "prod"
}

variable "alb_priority" {}
variable "alb_listener_https_arn" {}
variable "alb_listener_http_arn" {}
variable "alb_cloudwatch_id" {}
variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}

variable "task_role_name" {}
