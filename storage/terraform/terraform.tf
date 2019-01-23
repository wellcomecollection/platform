# Terraform

terraform {
  required_version = ">= 0.9"

  backend "s3" {
    role_arn = "arn:aws:iam::760097843905:role/developer"

    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/archive.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}

provider "aws" {
  region  = "eu-west-1"
  version = "1.42.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/developer"
  }
}

# Data

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
