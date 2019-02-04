variable "namespace" {}

variable "subnets" {
  type = "list"
}

variable "vpc_id" {}
variable "aws_region" {}

variable "account_id" {}

variable "dlq_alarm_arn" {}

variable "es_works_index" {}

variable "rds_ids_access_security_group_id" {}

variable "release_label" {}

variable "miro_adapter_topic_names" {
  type = "list"
}

variable "sierra_adapter_topic_names" {
  type = "list"
}

variable "vhs_miro_read_policy" {}
variable "vhs_sierra_read_policy" {}
