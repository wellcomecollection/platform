variable "goobi_items_queue_name" {}
variable "goobi_items_bucket_name" {}
variable "goobi_items_topic" {}

variable "release_id" {}
variable "ecr_repository_url" {}
variable "dlq_alarm_arn" {}
variable "cluster_name" {}

//variable "ecs_launch_type" {}
variable "account_id" {}

variable "vpc_id" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "alb_cloudwatch_id" {}
variable "alb_listener_https_arn" {}
variable "alb_listener_http_arn" {}
variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}
variable "goobi_vhs_tablename" {}
variable "goobi_vhs_bucketname" {}
variable "goobi_vhs_full_access_policy" {}

