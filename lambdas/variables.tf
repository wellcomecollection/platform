variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "dash_bucket" {
  description = "S3 bucket hosting our dashboard"
  default     = "wellcome-platform-dash"
}
