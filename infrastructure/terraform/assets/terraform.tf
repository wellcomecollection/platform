terraform {
  required_version = ">= 0.11"

  backend "s3" {
    role_arn = "arn:aws:iam::760097843905:role/developer"

    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/assets.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}

provider "aws" {
  region  = "${var.aws_region}"
  version = "1.10.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/admin"
  }
}
