variable "service_name" {}

variable "security_group_ids" {
  type = "list"
}

variable "cluster_id" {}
variable "vpc_id" {}

variable "private_subnets" {
  type = "list"
}

variable "namespace_id" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "container_image" {}
variable "container_port" {}

variable "env_vars" {
  type    = "map"
  default = {}
}

variable "env_vars_length" {}

variable "command" {
  type    = "list"
  default = []
}
