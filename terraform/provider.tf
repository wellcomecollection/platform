# Specify the provider and access details
provider "aws" {
  region = "${var.aws_region}"

  # v0.1.2 and 0.1.3 have a panic caused by S3 lifecycle rules.
  # We're pinning the version until
  # https://github.com/terraform-providers/terraform-provider-aws/pull/1316
  # is merged.
  # TODO: Remove this pin.
  version = "0.1.1"
}

data "aws_caller_identity" "current" {}
