# Specify the provider and access details
provider "aws" {
  region = "${var.aws_region}"

  version = "1.3.1"
}

data "aws_caller_identity" "current" {}
