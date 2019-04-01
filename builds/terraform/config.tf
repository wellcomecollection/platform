data "aws_ssm_parameter" "pypi_password" {
  name = "/aws/reference/secretsmanager/builds/pypi_password"
}

data "aws_ssm_parameter" "pypi_username" {
  name = "/builds/config/prod/pypi_username"
}

data "aws_ssm_parameter" "github_oauth_token" {
  name = "/aws/reference/secretsmanager/builds/github_oauth_token"
}

data "aws_ssm_parameter" "non_critical_slack_webhook" {
  name = "/aws/reference/secretsmanager/builds/non_critical_slack_webhook"
}

locals {
  pypi_username = "${data.aws_ssm_parameter.pypi_username.value}"
  pypi_password = "${data.aws_ssm_parameter.pypi_password.value}"

  github_oauth_token = "${data.aws_ssm_parameter.github_oauth_token.value}"

  non_critical_slack_webhook = "${data.aws_ssm_parameter.non_critical_slack_webhook.value}"
}
