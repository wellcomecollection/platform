variable "service_name" {
  description = "Name of the ECS service to create"
}

variable "cluster_id" {
  description = "ID of the cluster which this service should run in"
}

variable "task_definition_arn" {
  description = "ARN of the task definition to run in this service"
}

variable "container_name" {
  description = "Primary container to expose for service"
}

variable "container_port" {
  description = "Port on primary container to expose for service"
  default     = "8888"
}

variable "vpc_id" {
  description = "ID of VPC to run target_group in"
}

variable "listener_arn" {
  description = "ARN of listener for listener rule"
}

variable "path_pattern" {
  description = "path pattern to match for listener rule"
  default     = "/*"
}
