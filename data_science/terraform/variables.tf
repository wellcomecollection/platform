variable "key_name" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "vpc_cidr_block" {
  default = "18.0.0.0/16"
}

variable "namespace" {
  default = "datascience"
}

variable "admin_cidr_ingress" {
  type = "list"
}