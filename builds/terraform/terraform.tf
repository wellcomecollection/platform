terraform {
  required_version = ">= 0.11"

  backend "s3" {
    role_arn = "arn:aws:iam::760097843905:role/developer"

    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/builds.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
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

provider "aws" {
  region  = "${var.aws_region}"
  version = "1.27.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/admin"
  }
}

provider "aws" {
  alias = "storage"

  region  = "${var.aws_region}"
  version = "1.27.0"

  assume_role {
    role_arn = "arn:aws:iam::975596993436:role/admin"
  }
}

provider "aws" {
  alias = "catalogue"

  region  = "${var.aws_region}"
  version = "1.27.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/admin"
  }
}

provider "github" {
  token        = "${var.github_api_token}"
  organization = "wellcometrust"
}
