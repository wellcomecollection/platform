variable "name" {}
variable "vpc_id" {}
variable "aws_region" {}
variable "alb_listener_arn" {}
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
  default = "3960"
}

variable "memory" {
  default = "7350"
}

variable "task_desired_count" {
  default = "2"
}

variable "app_container_image" {}

variable "app_container_port" {
  default = "8888"
}

variable "app_cpu" {
  default = "1024"
}

variable "app_memory" {
  default = "2048"
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
  default = "128"
}

variable "sidecar_env_vars" {
  description = "Environment variables to pass to the container"
  type        = "map"
  default     = {}
}

variable "alb_id" {}
variable "cluster_id" {}
variable "namespace_id" {}