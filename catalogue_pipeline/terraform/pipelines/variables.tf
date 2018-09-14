variable "namespace" {}

variable "account_id" {}

variable "aws_region" {}

variable "messages_bucket" {}

variable "infra_bucket" {}

variable "index_v1" {}

variable "index_v2" {}

variable "es_cluster_credentials" {
  type = "map"
}

variable "service_egress_security_group_id" {}

variable "vhs_bucket_name" {}

variable "transformer_miro_container_image" {}

variable "id_minter_container_image" {}

variable "ingestor_container_image" {}

variable "transformer_sierra_container_image" {}

variable "recorder_container_image" {}

variable "matcher_container_image" {}

variable "merger_container_image" {}

variable "rds_access_security_group_id" {}

variable "identifiers_rds_cluster_password" {}

variable "identifiers_rds_cluster_username" {}

variable "identifiers_rds_cluster_port" {}

variable "identifiers_rds_cluster_host" {}

variable "private_subnets" {
  type = "list"
}

variable "vpc_id" {}

variable "vhs_miro_read_policy" {}

variable "vhs_sierra_read_policy" {}

variable "dlq_alarm_arn" {}

variable "lambda_error_alarm_arn" {}

variable "miro_adapter_topic_names" {
  type = "list"
}

variable "miro_adapter_topic_count" {}

variable "sierra_adapter_topic_names" {
  type = "list"
}

variable "sierra_adapter_topic_count" {}
