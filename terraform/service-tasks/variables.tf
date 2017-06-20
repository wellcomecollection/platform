variable "name" {
  description = "Name of the ECS service and task to create"
}

variable "alb_priority" {
  description = "ALB listener rule priority"
  default     = "100"
}

variable "desired_count" {
  description = "Desired task count per service"
  default     = "1"
}

variable "cluster_id" {
  description = "ID of the cluster which this service should run in"
}

variable "container_definitions" {

}

variable "task_role_arn" {
  description = "ARN of the task definition to run in this service"
}

variable "volume_name" {
  description = "Name of volume to mount (if required)"
  default     = "ephemera"
}

variable "volume_host_path" {
  description = "Location of mount point on host path (if required)"
  default     = "/tmp"
}

variable "container_name" {
  description = "Primary container to expose for service"
  default     = "nginx"
}

variable "container_port" {
  description = "Port on primary container to expose for service"
  default     = "9000"
}

variable "vpc_id" {
  description = "ID of VPC to run service in"
}


variable "listener_arn" {
  description = "ARN of listener for listener rule"
}

variable "path_pattern" {
  description = "path pattern to match for listener rule"
  default     = "/*"
}

variable "healthcheck_path" {
  description = "path for ECS healthcheck endpoint"
  default     = "/management/healthcheck"
}


