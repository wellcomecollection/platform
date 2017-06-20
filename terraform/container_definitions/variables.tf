variable "aws_region" {
  description = "AWS Region the task will run in"
  default     = "eu-west-1"
}

variable "name" {
  description = "Name of the task to create"
}

variable "template_name" {
  description = "Name of the template to use"
  default     = "default"
}

variable "nginx_uri" {
  description = "URI of container image for nginx"
  default = ""
}

variable "app_uri" {
  description = "URI of container image for app"
  default = ""
}

variable "volume_name" {
  description = "Name of volume to mount (if required)"
  default     = "ephemera"
}

variable "config_key" {
  description = "Location of config file within S3"
  default = ""
}

variable "infra_bucket" {
  description = "Location of infra bucket in S3"
  default = ""
}

variable "docker_image" {
  description = "Name of the docker image to run"
  default = ""
}

variable "container_port" {
  description = "Port exposed by the container"
  default = ""
}

variable "container_path" {
  description = "Path of the mounted volume in the docker container"
  default = ""
}

variable "environment_vars" {
  description = "Environment variables to pass to the container"
  default = ""
}
