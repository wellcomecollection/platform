terraform {
  required_version = ">= 0.9"

  backend "s3" {
    role_arn = "arn:aws:iam::760097843905:role/developer"

    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/shared_infra.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}

# Providers

provider "aws" {
  region  = "${local.aws_region}"
  version = "1.10.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/developer"
  }
}

provider "aws" {
  alias   = "storage"
  region  = "${local.aws_region}"
  version = "1.10.0"

  assume_role {
    role_arn = "arn:aws:iam::975596993436:role/developer"
  }
}

data "aws_caller_identity" "current" {}
