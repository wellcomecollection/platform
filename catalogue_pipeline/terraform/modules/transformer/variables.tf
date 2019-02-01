variable "namespace" {}
variable "namespace_id" {}

variable "container_image" {}

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

variable "vpc_id" {}

variable "subnets" {
  type = "list"
}

variable "transformed_works_topic_arn" {}

variable "message_bucket_name" {}

variable "adapter_topic_names" {
  type = "list"
}

variable "adapter_topic_count" {}
