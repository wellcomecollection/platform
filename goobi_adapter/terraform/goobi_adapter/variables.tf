variable "goobi_mets_queue_name" {}
variable "goobi_mets_bucket_name" {}
variable "goobi_mets_topic" {}

variable "release_id" {}
variable "ecr_repository_url" {}
variable "dlq_alarm_arn" {}
variable "cluster_name" {}

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
variable "vhs_goobi_tablename" {}
variable "vhs_goobi_bucketname" {}
variable "vhs_goobi_full_access_policy" {}
