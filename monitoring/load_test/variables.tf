variable "lambda_error_alarm_arn" {}
variable "every_5_minutes_name" {}
variable "ecs_services_cluster_id" {}

variable "release_ids" {
  type = "map"
}

variable "aws_region" {}
variable "bucket_dashboard_id" {}
variable "bucket_dashboard_arn" {}
