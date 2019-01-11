variable "namespace" {}
variable "vpc_id" {}

variable "subnets" {
  type = "list"
}

variable "cluster_name" {}
variable "cluster_id" {}

variable "namespace_id" {}

variable "aws_region" {}

variable "dlq_alarm_arn" {}

variable "account_id" {}

variable "messages_bucket" {}

variable "identifiers_rds_cluster_host" {}

variable "identifiers_rds_cluster_port" {}

variable "identifiers_rds_cluster_username" {}

variable "identifiers_rds_cluster_password" {}

variable "es_cluster_credentials" {
  type = "map"
}

variable "id_minter_container_image" {}

variable "ingestor_container_image" {}

variable "index" {}

variable "rds_access_security_group_id" {}

variable "service_egress_security_group_id" {}

variable "transformed_works_topic_name" {}

variable "allow_s3_messages_put_json" {}

variable "allow_cloudwatch_push_metrics_json" {}

variable "allow_s3_messages_get_json" {}
