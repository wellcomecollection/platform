variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

# We can't look up the ARN of our IIIF ACM certificate using the Terraform
# data providers because most of our infrastructure runs in eu-west-1, but
# the certificate for CloudFront has to live in us-east-1.
#
# Quoting http://docs.aws.amazon.com/acm/latest/userguide/acm-regions.html:
#
#     To use an ACM Certificate with Amazon CloudFront, you must request or
#     import the certificate in the US East (N. Virginia) region.
#     ACM Certificates in this region that are associated with a CloudFront
#     distribution are distributed to all the geographic locations configured
#     for that distribution.
#
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
