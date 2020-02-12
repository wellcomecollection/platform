variable "name" {
  description = "Name of the associated library e.g. 'storage'"
}

variable "bucket_arn" {
  description = "ARN of the S3 bucket used for releases"
}

variable "repo_name" {
  default = ""
}

variable "platform_read_only_role" {}