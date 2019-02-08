variable "infra_bucket_arn" {
  description = "ARN of the S3 bucket used for the release markers in the platform"
}

variable "sbt_releases_bucket_arn" {
  description = "ARN of the S3 bucket that holds sbt releases"
}

variable "lambda_pushes_topic_name" {}
variable "ecr_pushes_topic_name" {}

variable "repo_name" {}
