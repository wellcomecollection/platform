variable "aws_region" {
  default = "eu-west-1"
}

variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"
}

variable "infra_bucket" {
  default = "wellcomecollection-platform-infra"
}

variable "bagger_source_bucket_name" {}
