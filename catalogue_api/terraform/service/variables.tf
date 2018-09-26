variable "name" {}
variable "vpc_id" {}
variable "aws_region" {}
variable "alb_listener_arn_https" {}
variable "host_name" {}

variable "private_subnets" {
  type = "list"
}

variable "healthcheck_path" {
  default = "/management/healthcheck"
}

variable "path_pattern" {
  default = "/catalogue/*"
}

variable "log_group_prefix" {
  description = "Cloudwatch log group name prefix"
  default     = "catalogue-api"
}

variable "cpu" {
  default = "2048"
}

variable "memory" {
  default = "4096"
}

variable "task_desired_count" {
  default = "2"
}

variable "app_container_image" {}

variable "app_container_port" {
  default = "8888"
}

variable "app_cpu" {
  default = "1920"
}

variable "app_memory" {
  default = "4032"
}

variable "app_env_vars" {
  description = "Environment variables to pass to the container"
  type        = "map"
  default     = {}
}

variable "sidecar_container_image" {}

variable "sidecar_container_port" {
  default = "9000"
}

variable "sidecar_cpu" {
  default = "128"
}

variable "sidecar_memory" {
  default = "64"
}

variable "sidecar_env_vars" {
  description = "Environment variables to pass to the container"
  type        = "map"
  default     = {}
}

variable "cluster_id" {}
variable "namespace_id" {}

variable "es_cluster_credentials" {
  type = "map"
}

variable "es_config" {
  type = "map"
}

variable "deployment_minimum_healthy_percent" {
  default = "50"
}

variable "alb_cloudwatch_id" {}

variable "alb_server_error_alarm_arn" {}

variable "alb_client_error_alarm_arn" {}

variable "enable_alb_alarm" {}

variable "alb_api_wc_service_lb_security_group_id" {}

variable "alb_api_wc_cloudwatch_id" {}