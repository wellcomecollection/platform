terraform {
  required_version = ">= 0.11"

  backend "s3" {
    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/assets.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}

provider "aws" {
  profile = "platform-admin"
  region  = "${var.aws_region}"
  version = "1.7.0"
}
