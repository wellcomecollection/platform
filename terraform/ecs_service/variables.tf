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
  description = "Container to expose for service"
}

variable "container_port" {
  description = "Container port to expose for service"
  default     = "8888"
}

variable "vpc_id" {
  description = "ID of VPC to run target_group in"
}

variable "alb_id" {
  description = "ID of ALB to attach listener to"
}

variable "acm_cert_arn" {
  description = "ARN of ACM cert to use for listener"
}
