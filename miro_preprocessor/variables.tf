variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "release_ids" {
  description = "Release tags for Miro preprocessor"
  type        = "map"
}
