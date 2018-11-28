variable "namespace" {}

variable "efs_id" {}
variable "efs_security_group_id" {}

variable "notebook_bucket_name" {}
variable "notebook_bucket_arn" {}

variable "key_name" {}
variable "aws_region" {}
variable "vpc_cidr_block" {}
variable "vpc_id" {}
variable "admin_cidr_ingress" {}

variable "public_subnets" {
  type = "list"
}

variable "private_subnets" {
  type = "list"
}
