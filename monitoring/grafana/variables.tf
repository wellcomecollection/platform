variable "vpc_id" {}
variable "aws_region" {}

variable "private_subnets" {
  type    = "list"
  default = []
}

variable "public_subnets" {
  type    = "list"
  default = []
}

variable "cluster_id" {}
variable "cluster_name" {}

variable "grafana_anonymous_enabled" {}
variable "grafana_anonymous_role" {}
variable "grafana_admin_user" {}
variable "grafana_admin_password" {}