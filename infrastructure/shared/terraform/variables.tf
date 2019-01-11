variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "infra_bucket" {
  description = "S3 bucket storing our configuration"
}
