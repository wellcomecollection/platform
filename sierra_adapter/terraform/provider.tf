provider "aws" {
  region  = "${var.aws_region}"
  version = "0.1.4"
}

data "aws_caller_identity" "current" {}
