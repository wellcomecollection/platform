data "aws_caller_identity" "current" {}

provider "aws" {
  region = "us-east-1"
  alias  = "us_east_1"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/developer"
  }
}
