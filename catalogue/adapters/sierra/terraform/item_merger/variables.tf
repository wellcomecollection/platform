variable "resource_type" {}
variable "release_id" {}
variable "merged_dynamo_table_name" {}
variable "updates_topic_name" {}
variable "cluster_name" {}
variable "vpc_id" {}

variable "dlq_alarm_arn" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "account_id" {}
variable "vhs_full_access_policy" {}
variable "bucket_name" {}

variable "subnets" {
  type = "list"
}

variable "namespace_id" {}
variable "interservice_security_group_id" {}
variable "service_egress_security_group_id" {}

variable "sierra_items_bucket" {}

variable "reindexed_items_topic_name" {}
