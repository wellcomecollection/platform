variable "env_vars" {
  type = "map"
}

variable "env_vars_length" {}

variable "vpc_id" {}

variable "subnets" {
  type = "list"
}

variable "aws_region" {
  default = "eu-west-1"
}

variable "container_image" {}

variable "namespace_id" {}

variable "cluster_name" {}
variable "cluster_id" {}

variable "service_name" {}

variable "service_egress_security_group_id" {}

variable "metric_namespace" {
  default = "storage"
}

variable "high_metric_name" {
  default = "empty"
}

variable "min_capacity" {
  default = "1"
}

variable "max_capacity" {
  default = "1"
}

variable "desired_task_count" {
  default = "1"
}

variable "security_group_ids" {
  type    = "list"
  default = []
}

variable "cpu" {
  default = 512
}

variable "memory" {
  default = 1024
}
