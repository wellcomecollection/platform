variable "task_name" {
  description = "Name of the task to create"
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

variable "container_path" {
  description = "Path of the mounted volume in the docker container"
  default     = "/data"
}

variable "env_vars" {
  description = "Environment variables to pass to the container"
  type        = "list"
}

variable "cpu" {
  description = "vCPU units to provision for the script"
  default     = 1024
}

variable "memory" {
  description = "Memory units to provision for the script"
  default     = 1024
}
