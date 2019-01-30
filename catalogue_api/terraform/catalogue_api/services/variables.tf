variable "remus_es_config" {
  type = "map"
}

variable "romulus_es_config" {
  type = "map"
}

variable "subnets" {
  type = "list"
}

variable "vpc_id" {}

variable "remus_container_image" {}
variable "romulus_container_image" {}
variable "container_port" {}

variable "nginx_container_image" {}
variable "nginx_container_port" {}

variable "cluster_name" {}

variable "namespace" {}
variable "namespace_id" {}

variable "nlb_arn" {}

variable "romulus_listener_port" {}
variable "remus_listener_port" {}

data "aws_vpc" "vpc" {
  id = "${var.vpc_id}"
}

variable "remus_task_number" {}
variable "romulus_task_number" {}
