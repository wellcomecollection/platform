variable "min_capacity" {
  default = 0
}

variable "max_capacity" {
  default = 3
}

variable "vpc_id" {}
variable "ecs_cluster_id" {}
variable "ecs_cluster_name" {}

variable "service_name" {}

variable "container_port" {
  default = "80"
}

variable "source_queue_arn" {}
variable "source_queue_name" {}

variable "subnets" {
  type = "list"
}

variable "launch_type" {
  default = "FARGATE"
}

variable "task_desired_count" {
  default = 1
}

variable "memory" {
  description = "How much memory to allocate to the app"
  default     = 1024
}

variable "cpu" {
  description = "How much CPU to allocate to the app"
  default     = 512
}

variable "aws_region" {
  description = "AWS Region the task will run in"
}

variable "container_name" {
  description = "Internal name of primary container"
  default     = "app"
}

variable "container_image" {
  description = "Container image to run"
}

variable "env_vars" {
  description = "Environment variables to pass to the container"
  type        = "map"
  default     = {}
}

variable "security_group_ids" {
  type    = "list"
  default = []
}

variable "namespace_id" {
  default = "ecs"
}

variable "env_vars_length" {
  default = 0
}

variable "command" {
  type    = "list"
  default = []
}
