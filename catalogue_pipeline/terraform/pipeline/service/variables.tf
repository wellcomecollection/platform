variable "cluster_name" {}

variable "service_name" {
}
variable "container_image" {
}
variable "interservice_security_group_id" {
}
variable "service_egress_security_group_id" {
}
variable "env_vars" {
  type = "map"
}
variable "aws_region" {
}
variable "vpc_id" {
}
variable "subnets" { }
variable "namespace_id" {}

variable "source_queue_name" {
}
variable "source_queue_arn" {
}