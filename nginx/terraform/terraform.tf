# Terraform config

terraform {
  required_version = ">= 0.10"

  backend "s3" {
    role_arn = "arn:aws:iam::760097843905:role/developer"

    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/nginx.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}

data "aws_caller_identity" "current" {}

#Providers

provider "aws" {
  region  = "eu-west-1"
  version = "1.42.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/developer"
  }
}