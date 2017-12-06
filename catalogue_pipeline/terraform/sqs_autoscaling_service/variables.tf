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
  type = map
}

variable "alb_cloudwatch_id" {}
variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}

