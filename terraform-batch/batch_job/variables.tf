variable "name" {
  default = "default"
}

variable "image_uri" {}
variable "job_role_arn" {}

variable "job_vars" {
  description = "Environment variables to pass to the container"
  type        = "list"
  default     = []
}

variable "memory" {
  default = 128
}

variable "vcpus" {
  default = 1
}

variable "command" {
  # Used to generate a string of the form:
  # [ "cmd", "-i", "Ref::param" ]
  type = "list"

  default = []
}
