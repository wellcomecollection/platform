provider "aws" {
  region  = "${var.aws_region}"
  version = "1.33.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/developer"
  }
}

provider "aws" {
  region = "us-east-1"
  alias  = "us_east_1"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/developer"
  }
}
