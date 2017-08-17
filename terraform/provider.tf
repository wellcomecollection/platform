# Specify the provider and access details
provider "aws" {
  region = "${var.aws_region}"
}

data "aws_caller_identity" "current" {}
