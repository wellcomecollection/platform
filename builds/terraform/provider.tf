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
