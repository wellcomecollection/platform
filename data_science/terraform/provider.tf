provider "aws" {
  region  = "eu-west-1"
  version = "1.22.0"
}

data "aws_caller_identity" "current" {}
