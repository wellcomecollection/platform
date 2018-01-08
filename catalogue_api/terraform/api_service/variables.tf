variable "name" {
  description = "remus/romulus"
}

variable "cluster_id" {}
variable "vpc_id" {}

variable "ecr_repository_api_url" {}
variable "ecr_repository_nginx_url" {}

variable "api_host" {}

variable "alb_listener_https_arn" {}
variable "alb_listener_http_arn" {}
variable "alb_priority" {}
variable "alb_cloudwatch_id" {}
variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}

variable "es_config" {
  type = "map"
}
