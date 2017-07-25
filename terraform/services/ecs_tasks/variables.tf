variable "task_name" {
  description = "Name of the task to create"
}

variable "template_name" {
  description = "Name of the template to use"
  default     = "default"
}

variable "nginx_uri" {
  description = "URI of container image for nginx"
}

variable "app_uri" {
  description = "URI of container image for app"
}

variable "aws_region" {
  description = "AWS Region the task will run in"
  default     = "eu-west-1"
}

variable "task_role_arn" {
  description = "ARN of IAM Role for task"
}

variable "volume_name" {
  description = "Name of volume to mount (if required)"
  default     = "ephemera"
}

variable "volume_host_path" {
  description = "Location of mount point on host path (if required)"
  default     = "/tmp"
}

variable "primary_container_port" {
  description = "Port exposed by the primary container"
  default     = ""
}

variable "secondary_container_port" {
  description = "Port exposed by the secondary container"
  default     = ""
}

variable "container_path" {
  description = "Path of the mounted volume in the docker container"
  default     = ""
}

variable "service_vars" {
  description = "Environment variables to pass to the container"
  type        = "list"
}

variable "extra_vars" {
  description = "Environment variables to pass to the container"
  type        = "list"
}

variable "memory" {
  description = "How much memory to allocate to the app"
  default     = 2048
}

variable "memory" {
  description = "How much CPU to allocate to the app"
  default     = 512
}
