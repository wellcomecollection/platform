variable "vpc_id" {}
variable "namespace" {}
variable "aws_region" {}
variable "private_subnets" {
  type = "list"
}
variable "public_subnets" {
  type = "list"
}
variable "key_name" {}
variable "ssh_controlled_ingress_sg" {}