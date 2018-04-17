provider "aws" {
  region  = "${var.aws_region}"
  version = "0.1.4"
}

provider "aws" {
  region = "us-east-1"
  alias  = "us_east_1"
}
