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
  alias   = "platform"
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
    role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"
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

provider "aws" {
  alias   = "experience"
  region  = "${local.aws_region}"
  version = "1.10.0"

  assume_role {
    role_arn = "arn:aws:iam::130871440101:role/experience-developer"
  }
}

provider "aws" {
  alias   = "digitisation"
  region  = "${local.aws_region}"
  version = "1.10.0"

  assume_role {
    role_arn = "arn:aws:iam::404315009621:role/digitisation-developer"
  }
}

provider "aws" {
  alias   = "workflow"
  region  = "${local.aws_region}"
  version = "1.10.0"

  assume_role {
    role_arn = "arn:aws:iam::299497370133:role/workflow-developer"
  }
}

provider "aws" {
  alias   = "reporting"
  region  = "${local.aws_region}"
  version = "1.10.0"

  assume_role {
    role_arn = "arn:aws:iam::269807742353:role/reporting-developer"
  }
}