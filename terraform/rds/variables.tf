variable "cluster_identifier" {}

variable "database_name" {}

variable "username" {}

variable "password" {}

variable "vpc_security_group_ids" {
  type = "list"
}

variable "vpc_subnet_ids" {
  type = "list"
}

variable "vpc_id" {}

variable "admin_cidr_ingress" {}

variable "db_access_security_group" {}