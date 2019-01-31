variable "cluster_name" {}
variable "cluster_id" {}

variable "service_name" {}

variable "container_image" {}

variable "service_egress_security_group_id" {}

variable "env_vars" {
  type = "map"
}

variable "env_vars_length" {}

variable "aws_region" {}

variable "vpc_id" {}

variable "subnets" {
  type = "list"
}

variable "namespace_id" {}

variable "source_queue_name" {}

variable "source_queue_arn" {}

variable "max_capacity" {
  default = 5
}

variable "security_group_ids" {
  type    = "list"
  default = []
}
