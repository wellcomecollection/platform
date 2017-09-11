# Specify the provider and access details
provider "aws" {
  region = "${var.aws_region}"

  version = "0.1.4"
}