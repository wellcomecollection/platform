provider "aws" {
  region  = "${var.aws_region}"
  version = "1.27.0"
}

# You need to set the GITHUB_TOKEN environment variable to use this provider;
# there isn't a personal access token in our tfvars because they're per-user,
# not organisation-wide.
#
provider "github" {
  organization = "wellcometrust"
}
