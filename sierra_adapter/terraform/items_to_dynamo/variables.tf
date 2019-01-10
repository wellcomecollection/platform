variable "release_id" {}
variable "demultiplexer_topic_name" {}

variable "cluster_name" {}
variable "vpc_id" {}

variable "dlq_alarm_arn" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "account_id" {}

variable "subnets" {
  type = "list"
}

variable "namespace_id" {}
variable "interservice_security_group_id" {}
variable "service_egress_security_group_id" {}
