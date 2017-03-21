variable "service_name" {
  description = "Name of the ECS service to create"
}

variable "cluster_id" {
  description = "ID of the cluster which this service should run in"
}

variable "task_name" {
  description = "Name of the task to create"
}

variable "template_name" {
  description = "Name of the template to use"
  default     = "default"
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
}

variable "container_port" {
  description = "Port on primary container to expose for service"
}

variable "vpc_id" {
  description = "ID of VPC to run service in"
}

variable "image_uri" {
  description = "URI of container image for primary container"
  default     = "hello-world"
}

variable "listener_arn" {
  description = "ARN of listener for listener rule"
}

variable "path_pattern" {
  description = "path pattern to match for listener rule"
  default     = "/*"
}
