variable "subnets" {
  type = "list"
}

variable "cluster_name" {}
variable "cluster_id" {}

variable "namespace" {}
variable "namespace_id" {}
variable "namespace_tld" {}

variable "vpc_id" {}

variable "container_image" {}
variable "container_port" {}

variable "nginx_container_image" {}
variable "nginx_container_port" {}

variable "security_group_ids" {
  type = "list"
}

variable "env_vars_length" {}

variable "env_vars" {
  type = "map"
}

variable "lb_arn" {}
variable "listener_port" {}

variable "cpu" {
  default = 1024
}

variable "memory" {
  default = 2048
}

variable "sidecar_cpu" {
  default = "512"
}

variable "sidecar_memory" {
  default = "1024"
}

variable "app_cpu" {
  default = "512"
}

variable "app_memory" {
  default = "1024"
}

variable "aws_region" {
  default = "eu-west-1"
}

variable "launch_type" {
  default = "FARGATE"
}
