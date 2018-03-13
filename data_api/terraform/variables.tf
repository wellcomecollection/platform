variable "aws_region" {
  default = "eu-west-1"
}

variable "infra_bucket" {}

variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"
}

variable "key_name" {
  description = "Name of AWS key pair"
}
