variable "namespace" {}

variable "vpc_id" {}

variable "efs_id" {}
variable "efs_security_group_id" {}

variable "domain" {}

variable "public_subnets" {
  type = list(string)
}

variable "private_subnets" {
  type = list(string)
}

variable "key_name" {}
variable "aws_region" {}
variable "admin_cidr_ingress" {}

variable "cluster_name" {}
variable "cluster_arn" {}
variable "namespace_id" {}

variable "grafana_version" {
  default = "6.4.4"
}

variable "grafana_anonymous_enabled" {}
variable "grafana_anonymous_role" {}
variable "grafana_admin_user" {}
variable "grafana_admin_password" {}
