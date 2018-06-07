variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "release_ids" {
  type = "map"
}

variable "key_name" {
  description = "Name of AWS key pair"
}
