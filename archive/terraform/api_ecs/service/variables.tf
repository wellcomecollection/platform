variable "vpc_id" {}

variable "subnets" {
  type    = "list"
  default = []
}

variable "ecs_cluster_id" {}

variable "lb_listener_arn" {}

variable "namespace" {}

variable "api_container_image" {}

variable "api_container_port" {
}

variable task_cpu {}
variable "task_memory" {}
variable "api_cpu" {}
variable "api_memory" {}
variable "nginx_cpu" {}
variable "nginx_memory" {}


variable "app_env_vars" {
  description = "Environment variables to pass to the container"
  type        = "map"
  default     = {}
}

variable "nginx_container_image" {}

variable "nginx_container_port" {
  default = "9000"
}

variable "nginx_env_vars" {
  description = "Environment variables to pass to the container"
  type        = "map"
  default     = {}
}

variable "nginx_env_vars_length" {
  default = 0
}

variable "aws_region" {
  default = "eu-west-1"
}

variable "service_lb_security_group_id" {
  default = ""
}

variable "service_discovery_namespace" {
  default = ""
}

variable "health_check_path" {}

variable "api_env_vars" {
  type        = "map"
  default     = {}
}

variable "api_env_vars_length" {
  default = 0
}
