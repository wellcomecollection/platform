variable "subnets" {
  type = "list"
}

variable "cluster_name" {}

variable "namespace" {}
variable "namespace_id" {}
variable "namespace_tld" {}

variable "vpc_id" {}

variable "container_image" {}
variable "container_port" {}

variable "nginx_container_image" {}
variable "nginx_container_port" {}

variable "security_group_ids" {
  type = "list"
}

variable "service_egress_security_group_id" {}
variable "env_vars_length" {}

variable "env_vars" {
  type = "map"
}

variable "lb_arn" {}
variable "listener_port" {}
