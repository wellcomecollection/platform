data "aws_ssm_parameter" "es_user" {
  name = "/reporting/config/prod/es_user"
}

data "aws_ssm_parameter" "es_password" {
  name = "/aws/reference/secretsmanager/reporting/config/prod/es_password"
}

data "aws_ssm_parameter" "es_url" {
  name = "/reporting/config/prod/es_url"
}

locals {
  es_username = "${data.aws_ssm_parameter.es_user.value}"
  es_password = "${data.aws_ssm_parameter.es_password.value}"
  es_url      = "${data.aws_ssm_parameter.es_url.value}"
}
