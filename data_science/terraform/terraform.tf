# Terraform

terraform {
  required_version = ">= 0.9"

  backend "s3" {
    role_arn = "arn:aws:iam::760097843905:role/developer"

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

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/developer"
  }
}

# Data

data "aws_caller_identity" "current" {}

data "terraform_remote_state" "shared_infra" {
  backend = "s3"

  config {
    role_arn = "arn:aws:iam::760097843905:role/developer"

    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/shared_infra.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "infra_critical" {
  backend = "s3"

  config {
    role_arn = "arn:aws:iam::760097843905:role/developer"

    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/catalogue_pipeline_data.tfstate"
    region = "eu-west-1"
  }
}

locals {
  admin_cidr_ingress = "${data.terraform_remote_state.infra_critical.admin_cidr_ingress}"
}
