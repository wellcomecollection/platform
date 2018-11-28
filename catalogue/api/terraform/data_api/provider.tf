data "aws_caller_identity" "current" {}

provider "aws" {
  region = "us-east-1"
  alias  = "us_east_1"
}
