variable "lambda_error_alarm_arn" {}
variable "every_5_minutes_name" {}
variable "aws_ecs_cluster_services_id" {}

variable "release_ids" {
  type = "map"
}

variable "aws_region" {}
variable "bucket_dashboard_id" {}
variable "bucket_dashboard_arn" {}
