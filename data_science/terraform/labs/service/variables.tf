variable "vpc_cidr_block" {}
variable "vpc_id" {}
variable "subnets" {
  type    = "list"
  default = []
}

variable "ecs_cluster_id" {}

variable "lb_listener_arn" {}
variable "service_lb_security_group_id" {}

variable "namespace" {}

variable "container_image" {}

variable "container_port" {
  default = "80"
}

variable "service_discovery_namespace" {
  default = "labs"
}

variable "aws_region" {
  default = "eu-west-1"
}
