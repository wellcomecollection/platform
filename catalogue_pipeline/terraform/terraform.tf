provider "aws" {
  region  = "${local.aws_region}"
  version = "1.52.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/admin"
  }
}

terraform {
  required_version = ">= 0.9"

  backend "s3" {
    role_arn = "arn:aws:iam::760097843905:role/developer"

    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/catalogue_pipeline.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}

data "terraform_remote_state" "catalogue_pipeline_data" {
  backend = "s3"

  config {
    role_arn = "arn:aws:iam::760097843905:role/developer"

    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/catalogue_pipeline_data.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "shared_infra" {
  backend = "s3"

  config {
    role_arn = "arn:aws:iam::760097843905:role/developer"

    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/shared_infra.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "sierra_adapter" {
  backend = "s3"

  config {
    role_arn = "arn:aws:iam::760097843905:role/developer"

    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/sierra_adapter.tfstate"
    region = "eu-west-1"
  }
}

data "aws_caller_identity" "current" {}
