provider "aws" {
  region  = var.aws_region
  version = "~> 2.47.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/platform-admin"
  }
}

provider "aws" {
  alias = "storage"

  region  = var.aws_region
  version = "~> 2.47.0"

  assume_role {
    role_arn = "arn:aws:iam::975596993436:role/storage-admin"
  }
}

provider "aws" {
  alias = "catalogue"

  region  = var.aws_region
  version = "~> 2.47.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/platform-admin"
  }
}

provider "aws" {
  alias = "platform"

  region  = var.aws_region
  version = "~> 2.47.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/platform-admin"
  }
}

provider "aws" {
  alias = "workflow"

  region  = var.aws_region
  version = "~> 2.47.0"

  assume_role {
    role_arn = "arn:aws:iam::299497370133:role/workflow-admin"
  }
}

provider "github" {
  token        = var.github_oauth_token
  organization = "wellcometrust"
}
