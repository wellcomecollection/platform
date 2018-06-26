provider "aws" {
  region = "${var.aws_region}"

  version = "1.22.0"
}

data "aws_caller_identity" "current" {}
