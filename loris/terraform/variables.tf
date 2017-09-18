variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "iiif_acm_cert_arn" {
  description = "ARN of ACM cert for iiif API (in us-east-1) for CloudFront"
}
