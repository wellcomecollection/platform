variable "namespace" {}
variable "vpc_id" {
}

variable "subnets" {
  type = "list"
}

variable "aws_region" {
}

variable "dlq_alarm_arn" {
}
variable "account_id" {
}

variable "vhs_sourcedata_bucket_name" {
}
variable "messages_bucket" {
}

variable "matcher_graph_table_index" {
  description = "Name of the GSI in the matcher graph table"
  default     = "work-sets-index"
}

variable "matcher_lock_table_index" {
  description = "Name of the GSI in the matcher lock table"
  default     = "context-ids-index"
}

variable "infra_bucket" {
}
variable "lambda_error_alarm_arn" {
}


variable "identifiers_rds_cluster_host" {
}
variable "identifiers_rds_cluster_port" {
}
variable "identifiers_rds_cluster_username" {
}
variable "identifiers_rds_cluster_password" {
}

variable "es_cluster_credentials" {
  type = "map"
}

variable "vhs_sourcedata_read_policy" {
}
variable "transformer_container_image" {
}

variable "recorder_container_image" {
}

variable "matcher_container_image" {
}

variable "merger_container_image" {
}

variable "id_minter_container_image" {
}

variable "ingestor_container_image" {
}

variable "vhs_sourcedata_table_stream_arn" {
}