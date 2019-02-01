variable "namespace" {}

variable "subnets" {
  type = "list"
}

variable "vpc_id" {}
variable "aws_region" {}

variable "account_id" {}

variable "messages_bucket_arn" {}
variable "messages_bucket_id" {}

variable "vhs_recorder_bucket_id" {}

variable "dlq_alarm_arn" {}

variable "es_works_index" {}

variable "rds_ids_access_security_group_id" {}
variable "vhs_sierra_read_policy" {}

variable "release_label" {}

variable "miro_adapter_topic_names" {
  type = "list"
}

variable "miro_adapter_topic_count" {}

variable "sierra_adapter_topic_count" {}

variable "sierra_adapter_topic_names" {
  type = "list"
}
