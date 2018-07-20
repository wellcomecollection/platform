provider "aws" {
  region  = "${var.aws_region}"
  version = "1.27.0"
}

provider "github" {
  token        = "${var.github_api_token}"
  organization = "wellcometrust"
}
