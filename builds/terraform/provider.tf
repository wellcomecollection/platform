locals {
  catalogue_account_id = "756629837203"
  platform_account_id  = "760097843905"
  storage_account_id   = "975596993436"
  workflow_account_id  = "299497370133"

  account_ids = [
    local.catalogue_account_id,
    local.platform_account_id,
    local.storage_account_id,
    local.workflow_account_id,
  ]
}

provider "aws" {
  region  = var.aws_region
  version = "~> 2.47.0"

  assume_role {
    role_arn = "arn:aws:iam::${local.platform_account_id}:role/platform-admin"
  }
}

provider "aws" {
  alias = "storage"

  region  = var.aws_region
  version = "~> 2.47.0"

  assume_role {
    role_arn = "arn:aws:iam::${local.storage_account_id}:role/storage-admin"
  }
}

provider "aws" {
  alias = "catalogue"

  region  = var.aws_region
  version = "~> 2.47.0"

  assume_role {
    role_arn = "arn:aws:iam::${local.catalogue_account_id}:role/catalogue-admin"
  }
}

provider "aws" {
  alias = "platform"

  region  = var.aws_region
  version = "~> 2.47.0"

  assume_role {
    role_arn = "arn:aws:iam::${local.platform_account_id}:role/platform-admin"
  }
}

provider "aws" {
  alias = "workflow"

  region  = var.aws_region
  version = "~> 2.47.0"

  assume_role {
    role_arn = "arn:aws:iam::${local.workflow_account_id}:role/workflow-admin"
  }
}

provider "github" {
  token        = var.github_oauth_token
  organization = "wellcometrust"
}

provider "github" {
  alias = "collection"

  token        = var.github_oauth_token
  organization = "wellcomecollection"
}
