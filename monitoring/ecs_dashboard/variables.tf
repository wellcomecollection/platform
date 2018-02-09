variable "monitoring_bucket_id" {}
variable "lambda_error_alarm_arn" {}
variable "every_minute_arn" {}
variable "every_minute_name" {}

variable "dashboard_assumable_roles" {
  type = "list"
}
