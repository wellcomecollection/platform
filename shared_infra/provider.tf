# Specify the provider and access details
provider "aws" {
  region = "${var.aws_region}"

  version = "1.7.0"
}

data "aws_caller_identity" "current" {}
