variable "service_name" {}

variable "security_group_ids" {
  type = "list"
}

variable "cluster_name" {}
variable "vpc_id" {}

variable "private_subnets" {
  type = "list"
}

variable "namespace" {}
variable "namespace_id" {}
variable "namespace_tld" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "container_image" {}
variable "container_port" {}

variable "env_vars" {
  type    = "map"
  default = {}
}

variable "env_vars_length" {}

variable "command" {
  type    = "list"
  default = []
}

variable "nginx_container_image" {}

variable "nginx_container_port" {}

variable "service_egress_security_group_id" {}

variable "nginx_tcp_security_group_id" {}
