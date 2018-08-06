variable "resource_type" {}
variable "release_id" {}
variable "cluster_name" {}
variable "vpc_id" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "vhs_full_access_policy" {}

variable "subnets" {
  type = "list"
}

variable "namespace_id" {}
variable "interservice_security_group_id" {}
variable "service_egress_security_group_id" {}

variable "env_vars" {
  type = "map"
}

variable "env_vars_length" {}
