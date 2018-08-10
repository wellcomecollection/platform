variable "name" {}
variable "description" {}

variable "timeout" {
  default = "60"
}

variable "memory_size" {
  default = "1024"
}

variable "environment_variables" {
  type    = "map"
  default = {}
}

variable "lambda_error_alarm_arn" {}
variable "infra_bucket" {}
