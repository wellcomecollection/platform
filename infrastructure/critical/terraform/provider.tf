provider "aws" {
  region  = "${var.aws_region}"
  version = "1.46.0"
}

provider "aws" {
  alias = "us-east-1"

  region = "us-east-1"

  version = "1.46.0"
}

data "aws_caller_identity" "current" {}
