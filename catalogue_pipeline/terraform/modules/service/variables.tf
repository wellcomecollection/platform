variable "service_name" {}
variable "cluster_id" {}
variable "cluster_name" {}

variable "subnets" {
  type = "list"
}

variable "aws_region" {}
variable "namespace_id" {}
variable "container_image" {}

variable "secret_env_vars" {
  type = "map"
}

variable "secret_env_vars_length" {}

variable "env_vars" {
  type = "map"
}

variable "env_vars_length" {}

variable "security_group_ids" {
  default = []
  type    = "list"
}
