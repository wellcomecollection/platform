variable "es_config" {
  type = "map"
}

variable "subnets" {
  type = "list"
}

variable "cluster_name" {}

variable "namespace" {}
variable "namespace_id" {}

variable "vpc_id" {}

variable "container_image" {}
variable "container_port" {}

variable "nginx_container_image" {}
variable "nginx_container_port" {}

variable "security_group_ids" {
  type = "list"
}

variable "service_egress_security_group_id" {}

variable "lb_arn" {}
variable "listener_port" {}

variable "task_desired_count" {}
