variable "resource_type" {}

variable "windows_topic_arn" {}

variable "dlq_alarm_arn" {}

variable "cluster_name" {}
variable "cluster_id" {}

variable "vpc_id" {}

variable "aws_region" {
  default = "eu-west-1"
}
