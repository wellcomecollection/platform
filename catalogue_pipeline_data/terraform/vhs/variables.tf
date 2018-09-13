variable "name" {}

variable "infra_bucket" {
  default = "wellcomecollection-platform-infra"
}

variable "bucket_name_prefix" {
  default = "wellcomecollection-vhs-"
}

variable "table_name_prefix" {
  default = "vhs-"
}

variable "table_read_max_capacity" {
  default = 80
}

variable "table_write_max_capacity" {
  default = 300
}

variable "prevent_destroy" {
  default = "true"
}

variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "bucket_name" {
  default = ""
}

variable "account_id" {}
