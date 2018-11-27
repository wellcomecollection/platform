# Terraform config

terraform {
  required_version = ">= 0.10"

  backend "s3" {
    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/monitoring.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}

# Data

data "terraform_remote_state" "loris" {
  backend = "s3"

  config {
    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/loris.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "shared_infra" {
  backend = "s3"

  config {
    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/shared_infra.tfstate"
    region = "eu-west-1"
  }
}

data "aws_caller_identity" "current" {}

#Providers

provider "aws" {
  region  = "${var.aws_region}"
  version = "1.22.0"
}

provider "aws" {
  region = "us-east-1"
  alias  = "us_east_1"
}
