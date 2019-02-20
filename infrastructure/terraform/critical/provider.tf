provider "aws" {
  region  = "${var.aws_region}"
  version = "1.46.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/admin"
  }
}

provider "aws" {
  alias = "us-east-1"

  region = "us-east-1"

  version = "1.46.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/admin"
  }
}

data "aws_caller_identity" "current" {}
