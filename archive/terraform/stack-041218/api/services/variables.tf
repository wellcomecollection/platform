# Shared infra

variable "subnets" {
  type = "list"
}

variable "vpc_id" {}

variable "cluster_id" {}
variable "cluster_name" {}

variable "namespace" {}
variable "namespace_id" {}
variable "namespace_tld" {}

variable "nlb_arn" {}

data "aws_vpc" "vpc" {
  id = "${var.vpc_id}"
}

# Ingests Endpoint

variable "ingests_container_image" {}
variable "ingests_container_port" {}

variable "ingests_nginx_container_image" {}
variable "ingests_nginx_container_port" {}

variable "ingests_listener_port" {}

variable "ingests_env_vars" {
  type = "map"
}

variable "ingests_env_vars_length" {}

# Bags Endpoint

variable "bags_container_image" {}
variable "bags_container_port" {}

variable "bags_nginx_container_image" {}
variable "bags_nginx_container_port" {}

variable "bags_listener_port" {}

variable "bags_env_vars" {
  type = "map"
}

variable "bags_env_vars_length" {}

variable "interservice_security_group_id" {}
