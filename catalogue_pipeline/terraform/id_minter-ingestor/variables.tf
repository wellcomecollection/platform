variable "name" {}
variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"
}
variable "id_minter_repository_url" {}
variable "ingestor_repository_url" {}
variable "identifiers_rds_cluster" {
  type        = "map"
}

variable "cluster_name" {}
variable "vpc_id" {}
variable "services_alb" {
  type = "map"
}
variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}
variable "aws_region" {
}
variable "account_id" {}
variable "dlq_alarm_arn" {}
variable "es_config_ingestor" {
  type = "map"
}
variable "cloudwatch_push_metrics_policy_document" {}