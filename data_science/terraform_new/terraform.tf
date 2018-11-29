# Terraform

terraform {
  required_version = ">= 0.9"

  backend "s3" {
    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/data_science.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}

# Providers

provider "aws" {
  region  = "eu-west-1"
  version = "1.22.0"
}

# Data

data "aws_caller_identity" "current" {}

data "terraform_remote_state" "shared_infra" {
  backend = "s3"

  config {
    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/shared_infra.tfstate"
    region = "eu-west-1"
  }
}
