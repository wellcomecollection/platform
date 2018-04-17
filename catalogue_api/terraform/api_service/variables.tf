variable "name" {
  description = "remus/romulus"
}

variable "prod_api" {
  description = "remus/romulus"
}

variable "prod_api_release_id" {}
variable "prod_api_nginx_release_id" {}

variable "release_ids" {
  type = "map"
}

variable "cluster_id" {}
variable "vpc_id" {}

variable "ecr_repository_api_url" {}
variable "ecr_repository_nginx_url" {}

variable "api_prod_host" {}
variable "api_stage_host" {}

variable "alb_listener_https_arn" {}
variable "alb_listener_http_arn" {}
variable "alb_cloudwatch_id" {}
variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}

variable "es_config" {
  type = "map"
}
