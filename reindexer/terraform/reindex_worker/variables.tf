variable "namespace" {
  description = "miro, sierra, goobi, ..."
}

variable "vhs_table_arn" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "reindex_worker_container_image" {}

variable "service_egress_security_group_id" {}

variable "ecs_cluster_name" {}
variable "ecs_cluster_id" {}
variable "vpc_id" {}

variable "private_subnets" {
  type = "list"
}

variable "namespace_id" {}
