terraform {
  required_version = ">= 0.9"

  backend "s3" {
    role_arn = "arn:aws:iam::760097843905:role/platform-developer"

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
    role_arn = "arn:aws:iam::760097843905:role/platform-developer"
  }
}

provider "aws" {
  alias   = "storage"
  region  = "${local.aws_region}"
  version = "1.10.0"

  assume_role {
    role_arn = "arn:aws:iam::975596993436:role/storage-developer"
  }
}

provider "aws" {
  alias   = "catalogue"
  region  = "${local.aws_region}"
  version = "1.10.0"

  assume_role {
    role_arn = "arn:aws:iam::975596993436:role/catalogue-developer"
  }
}

provider "aws" {
  alias   = "datascience"
  region  = "${local.aws_region}"
  version = "1.10.0"

  assume_role {
    role_arn = "arn:aws:iam::964279923020:role/data-developer"
  }
}
