variable "goobi_mets_queue_name" {}
variable "goobi_mets_bucket_name" {}
variable "goobi_mets_topic" {}

variable "dlq_alarm_arn" {}

variable "account_id" {}

variable "vpc_id" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "vhs_goobi_tablename" {}
variable "vhs_goobi_bucketname" {}
variable "vhs_goobi_full_access_policy" {}

variable "service_name" {
  default = "goobi_reader"
}

variable "container_image" {}

variable "subnets" {
  type = "list"
}

variable "namespace" {}
