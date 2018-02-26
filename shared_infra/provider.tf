provider "aws" {
  region  = "${var.aws_region}"
  version = "1.10.0"
}

data "aws_caller_identity" "current" {}
