variable "namespace" {}

variable "namespace_id" {}
variable "source_name" {}

variable "lambda_error_alarm_arn" {}

variable "infra_bucket" {}

variable "aws_region" {}

variable "account_id" {}

variable "dlq_alarm_arn" {}

variable "transformed_works_topic_publish_policy" {}

variable "vhs_read_policy" {}

variable "allow_cloudwatch_push_metrics_json" {}

variable "allow_s3_messages_put_json" {}

variable "service_egress_security_group_id" {}

variable "cluster_name" {}
variable "cluster_id" {}

variable "subnets" {
  type = "list"
}

variable "vpc_id" {}

variable "transformed_works_topic_arn" {}

variable "transformer_container_image" {}

variable "messages_bucket" {}

variable "adapter_topic_names" {
  type = "list"
}

variable "adapter_topic_count" {}
