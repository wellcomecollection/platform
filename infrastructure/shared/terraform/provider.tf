provider "aws" {
  region  = "${var.aws_region}"
  version = "1.10.0"
}

provider "aws" {
  alias   = "storage"
  region  = "${var.aws_region}"
  version = "1.10.0"

  assume_role {
    role_arn = "arn:aws:iam::975596993436:role/developer"
  }
}

data "aws_caller_identity" "current" {}
