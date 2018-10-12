variable "vpc_id" {}

variable "subnets" {
  type    = "list"
  default = []
}

variable "ecs_cluster_id" {}

variable "lb_listener_arn" {}

variable "namespace" {}

variable "container_image" {}
variable "container_port" {}

variable "cpu" {}
variable "memory" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "interservice_security_group_id" {
  default = ""
}

variable "service_lb_security_group_id" {
  default = ""
}

variable "service_discovery_namespace" {
  default = ""
}

variable "health_check_path" {}

variable "env_vars" {
  type    = "map"
  default = {}
}

variable "env_vars_length" {
  default = 0
}
