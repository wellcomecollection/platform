variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"

  default = {
    api                = "b394bb3c551a0387d8058d6fa4ef20d2d6e29153"
    snapshot_generator = "b394bb3c551a0387d8058d6fa4ef20d2d6e29153"
   update_api_docs     = "61e8e94d9e1d4baaf1ac9cbe789e9112856f76a5"
  }
}
