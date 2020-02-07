variable "aws_region" {
  default = "eu-west-1"
}

# You can get an OAuth token at https://github.com/settings/tokens/new
#
# Your token needs the "repo" scopes.
#
# Don't commit it to the repository -- pass it when terraform asks for it.
variable "github_oauth_token" {
}
