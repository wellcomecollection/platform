variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "iiif_acm_cert_arn" {
  description = "ARN of ACM cert for iiif API (in us-east-1) for CloudFront"
}

variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"
}

variable "build_env" {
  description = "Build environment (prod, dev, stage, ...)"
  default     = "prod"
}

variable "infra_bucket" {
  description = "S3 bucket storing our configuration"
  default     = "platform-infra"
}

variable "key_name" {
  description = "Name of AWS key pair"
}
