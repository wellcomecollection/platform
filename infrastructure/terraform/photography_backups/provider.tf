provider "aws" {
  region  = "eu-west-1"
  version = "~> 2.47.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/platform-admin"
  }
}
