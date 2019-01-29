variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"

  default = {
    update_api_docs = "61e8e94d9e1d4baaf1ac9cbe789e9112856f76a5"
  }
}
