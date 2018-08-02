variable "aws_region" {
  default = "eu-west-1"
}

variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"
}