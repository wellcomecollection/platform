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
variable "certificate_domain" {}

variable "healthcheck_path" {}
variable "path_pattern" {}

variable "ebs_container_path" {}

variable "log_group_prefix" {
  description = "Cloudwatch log group name prefix"
  default     = "ecs"
}

variable "asg_min" {
  description = "Min number of instances"
  default     = "1"
}

variable "asg_desired" {
  description = "Desired number of instances"
  default     = "1"
}

variable "asg_max" {
  description = "Max number of instances"
  default     = "3"
}

variable "cpu" {}
variable "memory" {}

variable "task_desired_count" {
  default = "4"
}

variable "instance_type" {
  default = "c4.xlarge"
}

variable "app_container_image" {}
variable "app_container_port" {}
variable "app_cpu" {}
variable "app_memory" {}

variable "app_env_vars" {
  description = "Environment variables to pass to the container"
  type        = "map"
}

variable "sidecar_container_image" {}
variable "sidecar_container_port" {}
variable "sidecar_cpu" {}
variable "sidecar_memory" {}

variable "sidecar_env_vars" {
  description = "Environment variables to pass to the container"
  type        = "map"
}
