variable "dashboard_bucket" {}

variable "dashboard_assumable_roles" {
  type = "list"
}

variable "lambda_error_alarm_arn" {}
variable "every_minute_arn" {}
variable "every_minute_name" {}

variable "infra_bucket" {}
